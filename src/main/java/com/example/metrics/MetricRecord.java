package com.example.metrics;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * A single observation that stores raw indicator values before processing.
 */
public class MetricRecord {

    private final String id;
    private final Map<Indicator, Double> rawValues;

    public MetricRecord(String id, Map<Indicator, Double> rawValues) {
        this.id = id;
        this.rawValues = new EnumMap<>(rawValues);
    }

    public String getId() {
        return id;
    }

    public Map<Indicator, Double> getRawValues() {
        return Collections.unmodifiableMap(rawValues);
    }
}
