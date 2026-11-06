package org.forestFire.clustering;

import org.forestFire.data.FireRecord;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public final class ClusterMapper extends Mapper<LongWritable, Text, Text, Text> {
    private double[][] centroids;
    private double[] means;
    private double[] deviations;
    private double selectedDistance;

    @Override
    protected void setup(Context context) {
        int k = context.getConfiguration().getInt("forestfire.k", 0);
        centroids = parseMatrix(context.getConfiguration().get("forestfire.centroids"), k);
        means = parseVector(context.getConfiguration().get("forestfire.means"));
        deviations = parseVector(context.getConfiguration().get("forestfire.deviations"));
    }

    @Override
    public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        try {
            FireRecord record = FireRecord.parse(value.toString());
            double[] point = record.clusterFeatures();
            int cluster = closest(point);
            context.write(new Text(Integer.toString(cluster)),
                    new Text(serialize(record, selectedDistance)));
        } catch (IllegalArgumentException e) {
            context.getCounter("CSV", "REGISTROS_INVALIDOS").increment(1);
        }
    }

    private int closest(double[] point) {
        int selected = 0;
        double best = Double.POSITIVE_INFINITY;
        for (int cluster = 0; cluster < centroids.length; cluster++) {
            double distance = 0.0;
            for (int i = 0; i < point.length; i++) {
                double normalized = (point[i] - means[i]) / deviations[i];
                double delta = normalized - centroids[cluster][i];
                distance += delta * delta;
            }
            if (distance < best) {
                best = distance;
                selected = cluster;
            }
        }
        selectedDistance = best;
        return selected;
    }

    private String serialize(FireRecord record, double distance) {
        StringBuilder result = new StringBuilder();
        result.append(distance).append('|');
        for (int i = 0; i < FireRecord.CORRELATION_FEATURE_COUNT; i++) {
            if (i > 0) {
                result.append(',');
            }
            result.append(record.correlationValue(i));
        }
        return result.toString();
    }

    private static double[] parseVector(String value) {
        String[] values = value.split(",", -1);
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = Double.parseDouble(values[i]);
        }
        return result;
    }

    private static double[][] parseMatrix(String value, int rows) {
        String[] serializedRows = value.split(";", -1);
        double[][] result = new double[rows][];
        for (int i = 0; i < rows; i++) {
            result[i] = parseVector(serializedRows[i]);
        }
        return result;
    }
}
