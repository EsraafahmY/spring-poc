package com.poc.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageDiscountPolicy implements DiscountPolicy {

    private final int percent;

    public PercentageDiscountPolicy(int percent) {
        this.percent = percent;
    }

    @Override
    public BigDecimal apply(BigDecimal amount) {
        BigDecimal factor = BigDecimal.valueOf(100 - percent).divide(BigDecimal.valueOf(100));
        return amount.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String name() { return "PERCENTAGE_" + percent; }
}
