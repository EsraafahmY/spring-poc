package com.poc.order.web;

import com.poc.order.service.OrderService;
import com.poc.order.web.OrderDtos.CreateOrderRequest;
import com.poc.order.web.OrderDtos.OrderResponse;
import com.poc.order.web.OrderDtos.OrderSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** The logged-in user (from Spring Security) becomes the order owner. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req, Authentication auth) {
        return OrderResponse.from(service.placeOrder(auth.getName(), req));
    }

    @GetMapping("/my")
    public List<OrderResponse> myOrders(Authentication auth) {
        return service.ordersOf(auth.getName()).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/my/summary")
    public OrderSummary mySummary(Authentication auth) {
        return service.summary(auth.getName());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> all() {
        return service.all().stream().map(OrderResponse::from).toList();
    }
}
