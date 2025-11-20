package com.example.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implements the entropy weighting workflow on a region basis.
 */
public class EntropyWeightingProcessor {

    public record RegionScore(String recordId, Map<Indicator, Double> processedIndicators,
                              double rawScore, double zScore, double normalScore, double indexedScore) {
    }

    public record RegionWeights(String regionName, Map<Indicator, Double> weights, double compositeWeight) {
    }

    public List<RegionScore> processRegion(Region region) {
        Objects.requireNonNull(region, "region");
        List<MetricRecord> records = region.getRecords();
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<Indicator, Double>> processed = records.stream()
                .map(record -> ValueProcessor.clampAndTransform(record.getRawValues()))
                .toList();

        Map<Indicator, List<Double>> normalized = normalizeByIndicator(processed);
        Map<Indicator, Double> weights = computeWeights(normalized, records.size());

        List<Double> rawScores = computeRawScores(normalized, weights);
        double mean = Statistics.mean(rawScores);
        double std = Statistics.standardDeviation(rawScores, mean);

        List<RegionScore> scores = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            double rawScore = rawScores.get(i);
            double z = std == 0 ? 0.0 : (rawScore - mean) / std;
            double normalScore = Statistics.normalCdf(z);
            double indexed = normalScore * 100.0;
            scores.add(new RegionScore(records.get(i).getId(), processed.get(i), rawScore, z, normalScore, indexed));
        }
        return scores;
    }

    public RegionWeights summarizeWeights(Region region) {
        List<MetricRecord> records = region.getRecords();
        if (records.isEmpty()) {
            return new RegionWeights(region.getName(), Collections.emptyMap(), 0.0);
        }
        List<Map<Indicator, Double>> processed = records.stream()
                .map(record -> ValueProcessor.clampAndTransform(record.getRawValues()))
                .toList();
        Map<Indicator, List<Double>> normalized = normalizeByIndicator(processed);
        Map<Indicator, Double> weights = computeWeights(normalized, records.size());
        double compositeWeight = Indicator.compositeWeightMembers().stream()
                .mapToDouble(indicator -> weights.getOrDefault(indicator, 0.0))
                .sum();
        return new RegionWeights(region.getName(), weights, compositeWeight);
    }

    private Map<Indicator, List<Double>> normalizeByIndicator(List<Map<Indicator, Double>> processed) {
        Map<Indicator, List<Double>> normalized = new EnumMap<>(Indicator.class);
        for (Indicator indicator : Indicator.values()) {
            List<Double> values = processed.stream()
                    .map(map -> map.getOrDefault(indicator, 0.0))
                    .toList();
            double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double range = max - min;
            List<Double> normed = new ArrayList<>();
            if (range == 0) {
                for (int i = 0; i < values.size(); i++) {
                    normed.add(0.0);
                }
            } else if (indicator.getDirection() == Direction.POSITIVE) {
                for (double v : values) {
                    normed.add((v - min) / range);
                }
            } else {
                for (double v : values) {
                    normed.add((max - v) / range);
                }
            }
            normalized.put(indicator, normed);
        }
        return normalized;
    }

    private Map<Indicator, Double> computeWeights(Map<Indicator, List<Double>> normalized, int sampleSize) {
        double k = 1.0 / Math.log(sampleSize);
        Map<Indicator, Double> entropy = new EnumMap<>(Indicator.class);
        Map<Indicator, Double> weights = new EnumMap<>(Indicator.class);

        for (Map.Entry<Indicator, List<Double>> entry : normalized.entrySet()) {
            Indicator indicator = entry.getKey();
            List<Double> values = entry.getValue();
            double sum = values.stream().mapToDouble(Double::doubleValue).sum();
            double entropyValue;
            if (sum == 0.0) {
                entropyValue = 1.0; // maximum entropy when all probabilities are zero
            } else {
                double e = 0.0;
                for (double v : values) {
                    double p = v / sum;
                    if (p > 0) {
                        e += p * Math.log(p);
                    }
                }
                entropyValue = -k * e;
            }
            entropy.put(indicator, entropyValue);
        }

        double diffSum = entropy.values().stream().mapToDouble(e -> 1 - e).sum();

        for (Map.Entry<Indicator, Double> entry : entropy.entrySet()) {
            double diff = 1 - entry.getValue();
            double weight;
            if (diffSum == 0) {
                weight = 1.0 / entropy.size();
            } else {
                weight = diff / diffSum;
            }
            weights.put(entry.getKey(), weight);
        }
        return weights;
    }

    private List<Double> computeRawScores(Map<Indicator, List<Double>> normalized,
                                          Map<Indicator, Double> weights) {
        List<Double> scores = new ArrayList<>(normalized.values().iterator().next().size());
        for (int i = 0; i < scores.size(); i++) {
            scores.add(0.0);
        }
        for (Indicator indicator : Indicator.values()) {
            List<Double> values = normalized.get(indicator);
            double weight = weights.getOrDefault(indicator, 0.0);
            for (int i = 0; i < values.size(); i++) {
                scores.set(i, scores.get(i) + values.get(i) * weight);
            }
        }
        return scores;
    }
}
