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
        double[] sumX = new double[FireRecord.CORRELATION_FEATURE_COUNT];
        double[] sumY = new double[FireRecord.CORRELATION_FEATURE_COUNT];
        double[] sumX2 = new double[FireRecord.CORRELATION_FEATURE_COUNT];
        double[] sumY2 = new double[FireRecord.CORRELATION_FEATURE_COUNT];
        double[] sumXY = new double[FireRecord.CORRELATION_FEATURE_COUNT];

        for (Text value : values) {
            // distance|clusterFeatures(10, includes temp)|correlationFeatures(29, no temp)
            String[] serialized = value.toString().split("\\|", 3);
            double distance = Double.parseDouble(serialized[0]);
            double[] clusterFeatures = parseVector(serialized[1]);
            double[] correlationFeatures = parseVector(serialized[2]);
            count++;
            inertia += distance;
            double temperature = clusterFeatures[FireRecord.TEMPERATURE_INDEX];
            for (int i = 0; i < FireRecord.CLUSTER_FEATURE_COUNT; i++) {
                centroidSum[i] += (clusterFeatures[i] - means[i]) / deviations[i];
            }
            for (int i = 0; i < correlationFeatures.length; i++) {
                double feature = correlationFeatures[i];
                sumX[i] += temperature;
                sumY[i] += feature;
                sumX2[i] += temperature * temperature;
                sumY2[i] += feature * feature;
                sumXY[i] += temperature * feature;
            }
        }

        StringBuilder result = new StringBuilder();
        result.append(count).append('\t').append(serializeCentroid(centroidSum, count));
        result.append('\t').append(serializeCorrelations(
                count, sumX, sumY, sumX2, sumY2, sumXY));
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

    private String serializeCorrelations(long count, double[] sumX, double[] sumY,
                                         double[] sumX2, double[] sumY2, double[] sumXY) {
        StringBuilder result = new StringBuilder();
        String[] names = new String[sumY.length];
        System.arraycopy(FireRecord.NUMERIC_NAMES, 0, names, 0, FireRecord.NUMERIC_NAMES.length);
        for (int i = 0; i < FireRecord.MONTHS.length; i++) {
            names[FireRecord.NUMERIC_NAMES.length + i] = "month=" + FireRecord.MONTHS[i];
        }
        for (int i = 0; i < FireRecord.DAYS.length; i++) {
            names[FireRecord.NUMERIC_NAMES.length + FireRecord.MONTHS.length + i] =
                    "day=" + FireRecord.DAYS[i];
        }
        for (int i = 0; i < sumY.length; i++) {
            if (i > 0) {
                result.append(';');
            }
            double numerator = count * sumXY[i] - sumX[i] * sumY[i];
            double denominator = Math.sqrt(
                    (count * sumX2[i] - sumX[i] * sumX[i])
                            * (count * sumY2[i] - sumY[i] * sumY[i]));
            result.append(names[i]).append('=').append(
                    Double.isFinite(denominator) && denominator > 0.0
                    ? String.format(Locale.ROOT, "%.6f", numerator / denominator)
                    : "indefinida");
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
