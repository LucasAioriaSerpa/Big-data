package org.forestFire;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import org.forestFire.data.FireRecord;
import org.forestFire.clustering.ClusterMapper;
import org.forestFire.clustering.ClusterReducer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();

        if (args.length != 2) {
            System.err.println("Uso: Main <caminho_entrada> <diretorio_saida>");
            System.exit(2);
        }

        FileSystem fs = FileSystem.get(conf);
        List<FireRecord> records = readRecords(fs, new Path(args[0]));
        if (records.isEmpty()) {
            throw new IllegalArgumentException("Nenhum registro valido encontrado");
        }
        double[] means = means(records);
        double[] deviations = deviations(records, means);
        List<String> finalResults = new ArrayList<>();

        Random randK = new Random();
        int maxK = 20;
        int minK = 4;

        for (int k = 2; k <= (randK.nextInt(maxK - minK + 1) + minK); k++) {
            double[][] centroids = initializeKMeansPlusPlus(records, means, deviations, k);
            List<String> iterationResults = new ArrayList<>();
            for (int iteration = 0; iteration < 50; iteration++) {
                Path iterationPath = new Path(args[1] + "-work-" + k + "-" + iteration);
                Job job = createJob(conf, args[0], iterationPath, k, centroids, means, deviations);
                if (!job.waitForCompletion(true)) {
                    System.exit(1);
                }
                iterationResults = readResults(fs, iterationPath);
                double[][] next = centroidsFromResults(iterationResults, k, centroids);
                reinitializeEmptyClusters(iterationResults, next, records, means, deviations);
                fs.delete(iterationPath, true);
                if (movement(centroids, next) < 1e-4) {
                    centroids = next;
                    break;
                }
                centroids = next;
            }
            double silhouette = silhouette(records, centroids, means, deviations);
            boolean[] present = new boolean[k];
            for (String result : iterationResults) {
                finalResults.add("k=" + k + "\t" + result + "\t" + silhouette);
                present[Integer.parseInt(result.split("\t", -1)[0])] = true;
            }
            for (int cluster = 0; cluster < k; cluster++) {
                if (!present[cluster]) {
                    finalResults.add("k=" + k + "\t" + cluster + "\t0\t"
                            + serializeVector(centroids[cluster]) + "\t"
                            + emptyCorrelations() + "\t0.0\t"
                            + silhouette);
                }
            }
        }

        Path output = new Path(args[1]);
        fs.delete(output, true);
        fs.mkdirs(output);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                fs.create(new Path(output, "part-r-00000"))))) {
            for (String result : finalResults) {
                writer.write(result);
                writer.newLine();
            }
        }
    }

    private static Job createJob(Configuration base, String input, Path output, int k,
                                 double[][] centroids, double[] means, double[] deviations)
            throws Exception {
        Configuration conf = new Configuration(base);
        conf.setInt("forestfire.k", k);
        conf.set("forestfire.centroids", serializeMatrix(centroids));
        conf.set("forestfire.means", serializeVector(means));
        conf.set("forestfire.deviations", serializeVector(deviations));
        Job job = Job.getInstance(conf, "K-means++ e correlacoes k=" + k);
        job.setJarByClass(Main.class);
        job.setMapperClass(ClusterMapper.class);
        job.setReducerClass(ClusterReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(1);
        FileInputFormat.addInputPath(job, new Path(input));
        FileOutputFormat.setOutputPath(job, output);
        return job;
    }

    private static List<FireRecord> readRecords(FileSystem fs, Path input) throws Exception {
        List<FireRecord> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                fs.open(input)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    records.add(FireRecord.parse(line));
                } catch (IllegalArgumentException ignored) {
                    // The Mapper counts invalid records during the distributed pass.
                }
            }
        }
        return records;
    }

    private static List<String> readResults(FileSystem fs, Path output) throws Exception {
        List<String> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                fs.open(new Path(output, "part-r-00000"))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                results.add(line);
            }
        }
        return results;
    }

    private static double[] means(List<FireRecord> records) {
        double[] result = new double[FireRecord.CLUSTER_FEATURE_COUNT];
        for (FireRecord record : records) {
            double[] values = record.clusterFeatures();
            for (int i = 0; i < result.length; i++) result[i] += values[i];
        }
        for (int i = 0; i < result.length; i++) result[i] /= records.size();
        return result;
    }

    private static double[] deviations(List<FireRecord> records, double[] means) {
        double[] result = new double[means.length];
        for (FireRecord record : records) {
            double[] values = record.clusterFeatures();
            for (int i = 0; i < result.length; i++) {
                double delta = values[i] - means[i];
                result[i] += delta * delta;
            }
        }
        for (int i = 0; i < result.length; i++) {
            result[i] = Math.sqrt(result[i] / records.size());
            if (result[i] == 0.0) result[i] = 1.0;
        }
        return result;
    }

    private static double[][] initializeKMeansPlusPlus(List<FireRecord> records,
                                                        double[] means, double[] deviations, int k) {
        Random random = new Random(42L + k);
        double[][] points = new double[records.size()][];
        for (int i = 0; i < records.size(); i++) {
            double[] values = records.get(i).clusterFeatures();
            points[i] = new double[values.length];
            for (int j = 0; j < values.length; j++) {
                points[i][j] = (values[j] - means[j]) / deviations[j];
            }
        }
        double[][] result = new double[k][];
        boolean[] selected = new boolean[points.length];
        int first = random.nextInt(points.length);
        result[0] = points[first].clone();
        selected[first] = true;
        for (int c = 1; c < k; c++) {
            double[] distances = new double[points.length];
            double total = 0.0;
            for (int i = 0; i < points.length; i++) {
                distances[i] = selected[i] ? 0.0 : nearestDistance(points[i], result, c);
                total += distances[i];
            }
            double target = random.nextDouble() * total;
            int chosen = points.length - 1;
            for (int i = 0; i < distances.length; i++) {
                target -= distances[i];
                if (target <= 0.0) {
                    chosen = i;
                    break;
                }
            }
            result[c] = points[chosen].clone();
            selected[chosen] = true;
        }
        return result;
    }

    private static double nearestDistance(double[] point, double[][] centroids, int count) {
        double best = Double.POSITIVE_INFINITY;
        for (int c = 0; c < count; c++) {
            double distance = 0.0;
            for (int i = 0; i < point.length; i++) {
                double delta = point[i] - centroids[c][i];
                distance += delta * delta;
            }
            best = Math.min(best, distance);
        }
        return best;
    }

    private static double[][] centroidsFromResults(List<String> results, int k,
                                                   double[][] previous) {
        double[][] centroids = new double[k][];
        for (int i = 0; i < k; i++) centroids[i] = previous[i].clone();
        for (String result : results) {
            String[] fields = result.split("\t", -1);
            int cluster = Integer.parseInt(fields[0]);
            centroids[cluster] = parseVector(fields[2]);
        }
        return centroids;
    }

    private static void reinitializeEmptyClusters(List<String> results, double[][] centroids,
                                                  List<FireRecord> records, double[] means,
                                                  double[] deviations) {
        boolean[] present = new boolean[centroids.length];
        for (String result : results) {
            int cluster = Integer.parseInt(result.split("\t", -1)[0]);
            present[cluster] = true;
        }

        boolean[] selected = new boolean[records.size()];
        for (int cluster = 0; cluster < centroids.length; cluster++) {
            if (!present[cluster]) {
                int farthest = farthestAvailableRecord(records, selected, centroids, means, deviations);
                if (farthest >= 0) {
                    centroids[cluster] = normalizedPoint(records.get(farthest), means, deviations);
                    selected[farthest] = true;
                }
            }
        }
    }

    private static int farthestAvailableRecord(List<FireRecord> records, boolean[] selected,
                                               double[][] centroids, double[] means,
                                               double[] deviations) {
        int farthest = -1;
        double greatestDistance = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < records.size(); i++) {
            if (selected[i]) continue;
            double[] point = normalizedPoint(records.get(i), means, deviations);
            double distance = nearestDistance(point, centroids, centroids.length);
            if (distance > greatestDistance) {
                greatestDistance = distance;
                farthest = i;
            }
        }
        return farthest;
    }

    private static double[] normalizedPoint(FireRecord record, double[] means,
                                            double[] deviations) {
        double[] values = record.clusterFeatures();
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (values[i] - means[i]) / deviations[i];
        }
        return result;
    }

    private static double silhouette(List<FireRecord> records, double[][] centroids,
                                     double[] means, double[] deviations) {
        int[] assignments = new int[records.size()];
        double[][] points = new double[records.size()][];
        int[] clusterSizes = new int[centroids.length];
        for (int i = 0; i < records.size(); i++) {
            points[i] = normalizedPoint(records.get(i), means, deviations);
            assignments[i] = closestCluster(points[i], centroids);
            clusterSizes[assignments[i]]++;
        }

        double total = 0.0;
        for (int i = 0; i < points.length; i++) {
            int ownCluster = assignments[i];
            if (clusterSizes[ownCluster] < 2) continue;
            double ownDistance = 0.0;
            for (int j = 0; j < points.length; j++) {
                if (assignments[j] == ownCluster && i != j) {
                    ownDistance += euclideanDistance(points[i], points[j]);
                }
            }
            double a = ownDistance / (clusterSizes[ownCluster] - 1);
            double b = Double.POSITIVE_INFINITY;
            for (int cluster = 0; cluster < centroids.length; cluster++) {
                if (cluster == ownCluster || clusterSizes[cluster] == 0) continue;
                double otherDistance = 0.0;
                for (int j = 0; j < points.length; j++) {
                    if (assignments[j] == cluster) {
                        otherDistance += euclideanDistance(points[i], points[j]);
                    }
                }
                b = Math.min(b, otherDistance / clusterSizes[cluster]);
            }
            if (Double.isFinite(b) && Math.max(a, b) > 0.0) {
                total += (b - a) / Math.max(a, b);
            }
        }
        return total / records.size();
    }

    private static int closestCluster(double[] point, double[][] centroids) {
        int selected = 0;
        double best = Double.POSITIVE_INFINITY;
        for (int cluster = 0; cluster < centroids.length; cluster++) {
            double distance = euclideanDistance(point, centroids[cluster]);
            if (distance < best) {
                best = distance;
                selected = cluster;
            }
        }
        return selected;
    }

    private static double euclideanDistance(double[] left, double[] right) {
        double sum = 0.0;
        for (int i = 0; i < left.length; i++) {
            double delta = left[i] - right[i];
            sum += delta * delta;
        }
        return Math.sqrt(sum);
    }

    private static String emptyCorrelations() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < FireRecord.CORRELATION_FEATURE_COUNT; i++) {
            if (i > 0) result.append(';');
            result.append("indefinida");
        }
        return result.toString();
    }

    private static double movement(double[][] before, double[][] after) {
        double total = 0.0;
        for (int i = 0; i < before.length; i++) {
            double distance = 0.0;
            for (int j = 0; j < before[i].length; j++) {
                double delta = after[i][j] - before[i][j];
                distance += delta * delta;
            }
            total += distance;
        }
        return Math.sqrt(total);
    }

    private static String serializeVector(double[] values) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) result.append(',');
            result.append(values[i]);
        }
        return result.toString();
    }

    private static String serializeMatrix(double[][] values) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) result.append(';');
            result.append(serializeVector(values[i]));
        }
        return result.toString();
    }

    private static double[] parseVector(String value) {
        String[] values = value.split(",", -1);
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) result[i] = Double.parseDouble(values[i]);
        return result;
    }
}
