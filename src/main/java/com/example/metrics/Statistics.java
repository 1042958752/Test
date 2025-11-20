package com.example.metrics;

import java.util.List;

/**
 * Math helpers for entropy weighting and scoring.
 */
public final class Statistics {

    private Statistics() {
    }

    public static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public static double standardDeviation(List<Double> values, double mean) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    /**
     * Standard normal cumulative distribution function using an error function approximation.
     */
    public static double normalCdf(double z) {
        // Abramowitz and Stegun approximation for the error function
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));
        double tau = t * Math.exp(-z * z - 1.26551223 + t * (1.00002368
                + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806
                + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587
                + t * (-0.82215223 + t * 0.17087277)))))))));
        double erf = z >= 0 ? 1 - tau : tau - 1;
        return 0.5 * (1 + erf);
    }
}
