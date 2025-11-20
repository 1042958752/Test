package com.example.metrics;

import java.util.EnumSet;
import java.util.Set;

/**
 * Supported business indicators and their directional metadata.
 */
public enum Indicator {
    BEFORE_19_PI("19点前PI值", Direction.POSITIVE, Transformation.NONE, BoundaryRule.ZERO_TO_ONE),
    BEFORE_19_REPURCHASE_RATE("19点前复购率", Direction.POSITIVE, Transformation.NONE, BoundaryRule.ZERO_TO_ONE),
    ORDER_PENETRATION("订购渗透率", Direction.POSITIVE, Transformation.NONE, BoundaryRule.ZERO_TO_ONE),
    CUSTOMER_RETURN_RATE("顾客退货率", Direction.NEGATIVE, Transformation.SQUARE, BoundaryRule.NEG_ONE_TO_ZERO),
    STORE_RETURN_RATE("门店退货率", Direction.NEGATIVE, Transformation.SQUARE, BoundaryRule.NEG_ONE_TO_ZERO),
    SALES("销售额", Direction.POSITIVE, Transformation.SQUARE, BoundaryRule.NONE),
    PRICE_ELASTICITY("价格弹性", Direction.NEGATIVE, Transformation.NONE, BoundaryRule.NONE),
    SHORTAGE_RATE("少货率", Direction.NEGATIVE, Transformation.SQUARE, BoundaryRule.NONE),
    STORE_GROSS_PROFIT("门店毛利额", Direction.POSITIVE, Transformation.ABS_POW_ONE_POINT_FIVE, BoundaryRule.NONE),
    TIMESLOT_DISCOUNT("时段折扣额", Direction.NEGATIVE, Transformation.ABS_ONLY, BoundaryRule.NONE);

    private final String displayName;
    private final Direction direction;
    private final Transformation transformation;
    private final BoundaryRule boundaryRule;

    Indicator(String displayName, Direction direction, Transformation transformation, BoundaryRule boundaryRule) {
        this.displayName = displayName;
        this.direction = direction;
        this.transformation = transformation;
        this.boundaryRule = boundaryRule;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Direction getDirection() {
        return direction;
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public BoundaryRule getBoundaryRule() {
        return boundaryRule;
    }

    /**
     * Indicators that should be aggregated when reporting the additional weight column.
     */
    public static Set<Indicator> compositeWeightMembers() {
        return EnumSet.of(SALES, STORE_GROSS_PROFIT, TIMESLOT_DISCOUNT);
    }
}
