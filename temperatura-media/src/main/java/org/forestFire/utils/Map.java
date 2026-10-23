package org.forestFire.utils;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public class Map extends Mapper<Object, Text, Text, Text> {
    private static final int TEMPERATURE_INDEX = 8;
    private static final int EXPECTED_COLUMNS = 13;
    private static final int[] NUMERIC_COLUMNS = {0, 1, 4, 5, 6, 7, 9, 10, 11, 12};
    private static final String[] NUMERIC_NAMES = {
            "coluna 1 - X", "coluna 2 - Y", "coluna 5 - FFMC", "coluna 6 - DMC",
            "coluna 7 - DC", "coluna 8 - ISI", "coluna 10 - RH", "coluna 11 - wind",
            "coluna 12 - rain", "coluna 13 - area"
    };
    private static final String[] MONTHS = {
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    };
    private static final String[] DAYS = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};

    @Override
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String[] columns = value.toString().split(",", -1);
        if (columns.length != EXPECTED_COLUMNS) {
            countInvalidRecord(context);
            return;
        }

        try {
            double[] numericValues = new double[EXPECTED_COLUMNS];
            numericValues[TEMPERATURE_INDEX] = Double.parseDouble(columns[TEMPERATURE_INDEX].trim());
            if (!Double.isFinite(numericValues[TEMPERATURE_INDEX])) { countInvalidRecord(context); return; }
            for (int column : NUMERIC_COLUMNS) {
                numericValues[column] = Double.parseDouble(columns[column].trim());
                if (!Double.isFinite(numericValues[column])) { countInvalidRecord(context); return; }
            }

            String month = columns[2].trim().toLowerCase();
            String day = columns[3].trim().toLowerCase();
            if (!contains(MONTHS, month) || !contains(DAYS, day)) { countInvalidRecord(context); return; }

            double temperature = numericValues[TEMPERATURE_INDEX];
            for (int i = 0; i < NUMERIC_COLUMNS.length; i++) { emit(context, NUMERIC_NAMES[i], temperature, numericValues[NUMERIC_COLUMNS[i]]); }
            for (String category : MONTHS) { emit(context, "coluna 3 - mes=" + category, temperature, month.equals(category) ? 1.0 : 0.0); }
            for (String category : DAYS) { emit(context, "coluna 4 - dia=" + category, temperature, day.equals(category) ? 1.0 : 0.0); }
        } catch (NumberFormatException e) { countInvalidRecord(context); }
    }

    private void emit(Context context, String feature, double temperature, double value) throws IOException, InterruptedException {
        CorrelationStats stats = new CorrelationStats();
        stats.add(temperature, value);
        context.write(new Text(feature), new Text(stats.serialize()));
    }

    private boolean contains(String[] values, String target) {
        for (String value : values) { if (value.equals(target)) { return true; } }
        return false;
    }

    private void countInvalidRecord(Context context) { context.getCounter("CSV", "REGISTROS_INVALIDOS").increment(1); }
}