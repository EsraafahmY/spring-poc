package com.poc.product.web;

import com.poc.product.domain.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class ProductDtos {
    private ProductDtos() {}

    public record ProductRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @Min(0) int stock) {}

    public record ProductResponse(Long id, String name, BigDecimal price, int stock) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock());
        }
    }

    public record StockRequest(@Min(1) int quantity) {}
}
