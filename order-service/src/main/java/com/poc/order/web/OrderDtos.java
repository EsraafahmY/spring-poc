package com.poc.order.web;

import com.poc.order.domain.Order;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public final class OrderDtos {
    private OrderDtos() {}

    public record CreateOrderRequest(@NotNull Long productId, @Min(1) int quantity) {}

    public record OrderResponse(Long id, String username, Long productId, String productName, int quantity,
                                BigDecimal totalPrice, String discountPolicy, String status, Instant createdAt) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getUsername(), o.getProductId(), o.getProductName(),
                o.getQuantity(), o.getTotalPrice(), o.getDiscountPolicy(), o.getStatus().name(), o.getCreatedAt());
        }
    }

    public record OrderSummary(String username, long orders, BigDecimal totalSpent) {}
}
