package com.poc.order.service;

import java.math.BigDecimal;

public class NoDiscountPolicy implements DiscountPolicy {
    @Override public BigDecimal apply(BigDecimal amount) { return amount; }
    @Override public String name() { return "NO_DISCOUNT"; }
}
