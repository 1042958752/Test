package com.example.metrics;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Simple entry point demonstrating the full scoring workflow with sample data.
 */
public class Application {
    public static void main(String[] args) {
        Region east = new Region("华东", List.of(
                new MetricRecord("门店A", sampleValues(0.9, 0.8, 0.7, -0.2, -0.4, 120000, 1.2, 0.05, 38000, -3000)),
                new MetricRecord("门店B", sampleValues(1.2, 1.1, 0.6, -0.6, -0.2, 95000, 0.8, 0.1, 25000, -1800)),
                new MetricRecord("门店C", sampleValues(0.4, 0.5, 0.45, 0.05, -1.2, 78000, 1.5, 0.08, 19000, -4500))
        ));

        Region south = new Region("华南", List.of(
                new MetricRecord("门店D", sampleValues(0.7, 0.9, 0.8, -0.1, -0.05, 130000, 1.1, 0.03, 42000, -2500)),
                new MetricRecord("门店E", sampleValues(0.3, 0.2, 0.5, -0.7, -0.8, 68000, 0.9, 0.07, 15000, -1500))
        ));

        EntropyWeightingProcessor processor = new EntropyWeightingProcessor();

        System.out.println("====== 区域得分 ======");
        for (Region region : List.of(east, south)) {
            List<EntropyWeightingProcessor.RegionScore> scores = processor.processRegion(region);
            EntropyWeightingProcessor.RegionWeights weights = processor.summarizeWeights(region);

            System.out.println("区域: " + region.getName());
            for (EntropyWeightingProcessor.RegionScore score : scores) {
                System.out.printf("%s -> 原始综合得分 %.4f, 区域内正态0-1得分 %.4f, 区域内指数0-100得分 %.2f%n",
                        score.recordId(), score.rawScore(), score.normalScore(), score.indexedScore());
            }
            System.out.println("权重:");
            weights.weights().forEach((indicator, weight) ->
                    System.out.printf("  %s: %.4f%n", indicator.getDisplayName(), weight));
            System.out.printf("销售额+门店毛利额+时段折扣额权重和: %.4f%n", weights.compositeWeight());
            System.out.println();
        }
    }

    private static Map<Indicator, Double> sampleValues(double before19Pi, double repurchase, double penetration,
                                                       double customerReturn, double storeReturn, double sales,
                                                       double priceElasticity, double shortage, double grossProfit,
                                                       double timeslotDiscount) {
        Map<Indicator, Double> values = new EnumMap<>(Indicator.class);
        values.put(Indicator.BEFORE_19_PI, before19Pi);
        values.put(Indicator.BEFORE_19_REPURCHASE_RATE, repurchase);
        values.put(Indicator.ORDER_PENETRATION, penetration);
        values.put(Indicator.CUSTOMER_RETURN_RATE, customerReturn);
        values.put(Indicator.STORE_RETURN_RATE, storeReturn);
        values.put(Indicator.SALES, sales);
        values.put(Indicator.PRICE_ELASTICITY, priceElasticity);
        values.put(Indicator.SHORTAGE_RATE, shortage);
        values.put(Indicator.STORE_GROSS_PROFIT, grossProfit);
        values.put(Indicator.TIMESLOT_DISCOUNT, timeslotDiscount);
        return values;
    }
}
