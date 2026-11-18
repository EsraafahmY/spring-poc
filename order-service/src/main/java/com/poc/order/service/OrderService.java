package com.poc.order.service;

import com.poc.order.client.ProductClient;
import com.poc.order.client.ProductClient.ProductDto;
import com.poc.order.config.OrderProperties;
import com.poc.order.domain.Order;
import com.poc.order.repository.OrderRepository;
import com.poc.order.web.OrderDtos.CreateOrderRequest;
import com.poc.order.web.OrderDtos.OrderSummary;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * All collaborators (repository, Feign client, discount policy, properties) are injected
 * by the IoC container through the constructor.
 */
@Service
public class OrderService {

    private final OrderRepository repository;
    private final ProductClient productClient;
    private final DiscountPolicy discountPolicy;
    private final OrderProperties properties;

    public OrderService(OrderRepository repository, ProductClient productClient,
                        DiscountPolicy discountPolicy, OrderProperties properties) {
        this.repository = repository;
        this.productClient = productClient;
        this.discountPolicy = discountPolicy;
        this.properties = properties;
    }

    @Transactional
    public Order placeOrder(String username, CreateOrderRequest req) {
        if (req.quantity() > properties.maxQuantityPerOrder()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Max quantity per order is " + properties.maxQuantityPerOrder());
        }
        ProductDto product;
        try {
            product = productClient.getProduct(req.productId());
            productClient.decreaseStock(req.productId(), new ProductClient.StockRequest(req.quantity()));
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + req.productId() + " not found");
        } catch (FeignException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "product-service is unavailable, try again");
        }
        BigDecimal gross = product.price().multiply(BigDecimal.valueOf(req.quantity()));
        BigDecimal total = discountPolicy.apply(gross);
        return repository.save(new Order(username, product.id(), product.name(), req.quantity(),
            total, discountPolicy.name()));
    }

    @Transactional(readOnly = true)
    public List<Order> ordersOf(String username) {
        return repository.findByUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public List<Order> all() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public OrderSummary summary(String username) {
        return new OrderSummary(username, repository.findByUsernameOrderByCreatedAtDesc(username).size(),
            repository.totalSpentBy(username));
    }
}
