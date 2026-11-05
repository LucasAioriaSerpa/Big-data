package org.forestFire.data;

import java.util.Locale;

public final class FireRecord {
    public static final String[] NUMERIC_NAMES = {
            "X", "Y", "FFMC", "DMC", "DC", "ISI", "RH", "wind", "rain", "area"
    };
    public static final String[] MONTHS = {
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    };
    public static final String[] DAYS = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};
    public static final int[] NUMERIC_COLUMNS = {0, 1, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    public static final int CLUSTER_FEATURE_COUNT = 10; // excludes area, which is the fire outcome
    private static final int[] CORRELATION_NUMERIC_INDICES = {0, 1, 2, 3, 4, 5, 7, 8, 9, 10};
    public static final int CORRELATION_FEATURE_COUNT = NUMERIC_NAMES.length + MONTHS.length + DAYS.length;

    private final double[] numeric;
    private final String month;
    private final String day;

    private FireRecord(double[] numeric, String month, String day) {
        this.numeric = numeric;
        this.month = month;
        this.day = day;
    }

    public static FireRecord parse(String line) {
        String[] columns = line.split(",", -1);
        if (columns.length != 13) {
            throw new IllegalArgumentException("Esperadas 13 colunas");
        }

        double[] numeric = new double[NUMERIC_COLUMNS.length];
        for (int i = 0; i < NUMERIC_COLUMNS.length; i++) {
            numeric[i] = Double.parseDouble(columns[NUMERIC_COLUMNS[i]].trim());
            if (!Double.isFinite(numeric[i])) {
                throw new IllegalArgumentException("Valor numerico invalido");
            }
        }
        String month = columns[2].trim().toLowerCase(Locale.ROOT);
        String day = columns[3].trim().toLowerCase(Locale.ROOT);
        if (indexOf(MONTHS, month) < 0 || indexOf(DAYS, day) < 0) {
            throw new IllegalArgumentException("Categoria invalida");
        }
        return new FireRecord(numeric, month, day);
    }

    public double[] clusterFeatures() {
        double[] result = new double[CLUSTER_FEATURE_COUNT];
        System.arraycopy(numeric, 0, result, 0, CLUSTER_FEATURE_COUNT);
        return result;
    }

    public double temperature() {
        return numeric[6];
    }

    public double correlationValue(int index) {
        if (index < CORRELATION_NUMERIC_INDICES.length) {
            return numeric[CORRELATION_NUMERIC_INDICES[index]];
        }
        int monthIndex = index - CORRELATION_NUMERIC_INDICES.length;
        if (monthIndex < MONTHS.length) {
            return month.equals(MONTHS[monthIndex]) ? 1.0 : 0.0;
        }
        int dayIndex = monthIndex - MONTHS.length;
        return day.equals(DAYS[dayIndex]) ? 1.0 : 0.0;
    }

    private static int indexOf(String[] values, String target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(target)) {
                return i;
            }
        }
        return -1;
    }
}
