package com.example.metrics;

import java.util.EnumMap;
import java.util.Map;

/**
 * Applies boundary handling and indicator-specific transformations.
 */
public final class ValueProcessor {

    private ValueProcessor() {
    }

    public static Map<Indicator, Double> clampAndTransform(Map<Indicator, Double> raw) {
        Map<Indicator, Double> processed = new EnumMap<>(Indicator.class);
        for (Map.Entry<Indicator, Double> entry : raw.entrySet()) {
            Indicator indicator = entry.getKey();
            double value = entry.getValue();
            double clamped = applyBoundary(indicator.getBoundaryRule(), value);
            double transformed = applyTransformation(indicator.getTransformation(), clamped);
            processed.put(indicator, transformed);
        }
        return processed;
    }

    private static double applyBoundary(BoundaryRule rule, double value) {
        return switch (rule) {
            case NONE -> value;
            case ZERO_TO_ONE -> Math.max(0.0, Math.min(1.0, value));
            case NEG_ONE_TO_ZERO -> Math.max(-1.0, Math.min(0.0, value));
        };
    }

    private static double applyTransformation(Transformation transformation, double value) {
        return switch (transformation) {
            case NONE -> value;
            case SQUARE -> value * value;
            case ABS_POW_ONE_POINT_FIVE -> Math.pow(Math.abs(value), 1.5);
            case ABS_ONLY -> Math.abs(value);
        };
    }
}
