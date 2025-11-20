package com.example.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a group of metric records belonging to the same region.
 */
public class Region {
    private final String name;
    private final List<MetricRecord> records;

    public Region(String name, List<MetricRecord> records) {
        this.name = name;
        this.records = new ArrayList<>(records);
    }

    public String getName() {
        return name;
    }

    public List<MetricRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }
}
