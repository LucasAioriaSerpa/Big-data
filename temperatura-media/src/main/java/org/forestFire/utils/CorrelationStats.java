package org.forestFire.utils;

final class CorrelationStats {
    private long count;
    private double meanX;
    private double meanY;
    private double varianceX;
    private double varianceY;
    private double covariance;

    void add(double x, double y) {
        long nextCount = count + 1;
        double deltaX = x - meanX;
        double deltaY = y - meanY;
        meanX += deltaX / nextCount;
        meanY += deltaY / nextCount;
        varianceX += deltaX * (x - meanX);
        varianceY += deltaY * (y - meanY);
        covariance += deltaX * (y - meanY);
        count = nextCount;
    }

    void merge(CorrelationStats other) {
        if (other.count == 0) {
            return;
        }
        if (count == 0) {
            count = other.count;
            meanX = other.meanX;
            meanY = other.meanY;
            varianceX = other.varianceX;
            varianceY = other.varianceY;
            covariance = other.covariance;
            return;
        }

        long mergedCount = count + other.count;
        double deltaX = other.meanX - meanX;
        double deltaY = other.meanY - meanY;
        double mergeWeight = (double) count * other.count / mergedCount;

        varianceX += other.varianceX + deltaX * deltaX * mergeWeight;
        varianceY += other.varianceY + deltaY * deltaY * mergeWeight;
        covariance += other.covariance + deltaX * deltaY * mergeWeight;
        meanX += deltaX * other.count / mergedCount;
        meanY += deltaY * other.count / mergedCount;
        count = mergedCount;
    }

    double getCorrelation() {
        if (varianceX <= 0.0 || varianceY <= 0.0) { return Double.NaN; }
        return covariance / Math.sqrt(varianceX * varianceY);
    }

    long getCount() { return count; }

    String serialize() { return count + "," + meanX + "," + meanY + "," + varianceX + "," + varianceY + "," + covariance; }

    static CorrelationStats parse(String serialized) {
        String[] parts = serialized.split(",", -1);
        if (parts.length != 6) { throw new IllegalArgumentException("Estatistica de correlacao invalida"); }

        CorrelationStats stats = new CorrelationStats();
        stats.count = Long.parseLong(parts[0]);
        stats.meanX = Double.parseDouble(parts[1]);
        stats.meanY = Double.parseDouble(parts[2]);
        stats.varianceX = Double.parseDouble(parts[3]);
        stats.varianceY = Double.parseDouble(parts[4]);
        stats.covariance = Double.parseDouble(parts[5]);
        return stats;
    }
}