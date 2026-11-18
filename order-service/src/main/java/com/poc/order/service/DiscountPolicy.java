package com.poc.order.service;

import java.math.BigDecimal;

/**
 * Spring Core IoC & DI demo: OrderService depends on this abstraction only.
 * Which implementation gets injected is decided by the container (see PricingConfig).
 */
public interface DiscountPolicy {
    BigDecimal apply(BigDecimal amount);
    String name();
}
