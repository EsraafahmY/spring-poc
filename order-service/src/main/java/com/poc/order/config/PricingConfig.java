package com.poc.order.config;

import com.poc.order.service.DiscountPolicy;
import com.poc.order.service.NoDiscountPolicy;
import com.poc.order.service.PercentageDiscountPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Core: Java-based configuration. The container calls this @Bean method once (singleton)
 * and injects the result wherever a DiscountPolicy is needed. Change discount-percent in the
 * Config Server and the wiring changes without touching OrderService.
 */
@Configuration
public class PricingConfig {

    @Bean
    DiscountPolicy discountPolicy(OrderProperties props) {
        return props.discountPercent() > 0
            ? new PercentageDiscountPolicy(props.discountPercent())
            : new NoDiscountPolicy();
    }
}
