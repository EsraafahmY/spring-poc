package com.poc.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

/**
 * Spring Cloud: declarative REST client. "product-service" is the Eureka service name,
 * so there is no hard-coded host/port and calls are load balanced across instances.
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long id);

    @PostMapping("/api/products/{id}/decrease-stock")
    ProductDto decreaseStock(@PathVariable("id") Long id, @RequestBody StockRequest request);

    record ProductDto(Long id, String name, BigDecimal price, int stock) {}

    record StockRequest(int quantity) {}
}
