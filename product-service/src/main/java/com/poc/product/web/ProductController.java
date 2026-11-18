package com.poc.product.web;

import com.poc.product.service.ProductService;
import com.poc.product.web.ProductDtos.ProductRequest;
import com.poc.product.web.ProductDtos.ProductResponse;
import com.poc.product.web.ProductDtos.StockRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Spring Web MVC REST controller: JSON in, JSON out. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(defaultValue = "false") boolean inStock) {
        return service.findAll(inStock).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(@Valid @RequestBody ProductRequest req) {
        return ProductResponse.from(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return ProductResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /** Called by order-service (through Feign) when an order is placed. */
    @PostMapping("/{id}/decrease-stock")
    @PreAuthorize("hasRole('USER')")
    public ProductResponse decreaseStock(@PathVariable Long id, @Valid @RequestBody StockRequest req) {
        return ProductResponse.from(service.decreaseStock(id, req.quantity()));
    }
}
