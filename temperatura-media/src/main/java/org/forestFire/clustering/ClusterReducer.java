package org.forestFire.clustering;

import org.forestFire.data.FireRecord;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.Locale;

public final class ClusterReducer extends Reducer<Text, Text, Text, Text> {
    private double[] means;
    private double[] deviations;

    @Override
    protected void setup(Context context) {
        means = parseVector(context.getConfiguration().get("forestfire.means"));
        deviations = parseVector(context.getConfiguration().get("forestfire.deviations"));
    }

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        long count = 0;
        double inertia = 0.0;
        double[] centroidSum = new double[FireRecord.CLUSTER_FEATURE_COUNT];
        double[] sums = new double[FireRecord.CLUSTER_FEATURE_COUNT];
        double[] sumSquares = new double[FireRecord.CLUSTER_FEATURE_COUNT];
        double[][] sumProducts = new double[FireRecord.CLUSTER_FEATURE_COUNT]
                [FireRecord.CLUSTER_FEATURE_COUNT];

        for (Text value : values) {
            // distance|clusterFeatures(11, all numeric values)
            String[] serialized = value.toString().split("\\|", 2);
            double distance = Double.parseDouble(serialized[0]);
            double[] clusterFeatures = parseVector(serialized[1]);
            count++;
            inertia += distance;
            for (int i = 0; i < FireRecord.CLUSTER_FEATURE_COUNT; i++) {
                centroidSum[i] += (clusterFeatures[i] - means[i]) / deviations[i];
                sums[i] += clusterFeatures[i];
                sumSquares[i] += clusterFeatures[i] * clusterFeatures[i];
                for (int j = 0; j < FireRecord.CLUSTER_FEATURE_COUNT; j++) {
                    sumProducts[i][j] += clusterFeatures[i] * clusterFeatures[j];
                }
            }
        }

        StringBuilder result = new StringBuilder();
        result.append(count).append('\t').append(serializeCentroid(centroidSum, count));
        result.append('\t').append(serializeCorrelations(count, sums, sumSquares, sumProducts));
        result.append('\t').append(inertia);
        context.write(new Text(key.toString()), new Text(result.toString()));
    }

    private String serializeCentroid(double[] sum, long count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < sum.length; i++) {
            if (i > 0) {
                result.append(',');
            }
            result.append(sum[i] / count);
        }
        return result.toString();
    }

    private String serializeCorrelations(long count, double[] sums, double[] sumSquares,
                                         double[][] sumProducts) {
        StringBuilder result = new StringBuilder();
        for (int row = 0; row < FireRecord.CLUSTER_FEATURE_COUNT; row++) {
            for (int column = 0; column < FireRecord.CLUSTER_FEATURE_COUNT; column++) {
                if (row > 0 || column > 0) {
                    result.append(';');
                }
                double numerator = count * sumProducts[row][column]
                        - sums[row] * sums[column];
                double denominator = Math.sqrt(
                        (count * sumSquares[row] - sums[row] * sums[row])
                                * (count * sumSquares[column] - sums[column] * sums[column]));
                result.append(FireRecord.NUMERIC_NAMES[row])
                        .append('~')
                        .append(FireRecord.NUMERIC_NAMES[column])
                        .append('=')
                        .append(Double.isFinite(denominator) && denominator > 0.0
                                ? String.format(Locale.ROOT, "%.6f", numerator / denominator)
                                : "indefinida");
            }
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
}
