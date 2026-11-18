package com.poc.order.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Spring Configuration: business settings coming from the Config Server (order-service.yml). */
@Validated
@ConfigurationProperties(prefix = "poc.order")
public record OrderProperties(
    @Min(0) @Max(100) int discountPercent,
    @Min(1) int maxQuantityPerOrder) {
}
