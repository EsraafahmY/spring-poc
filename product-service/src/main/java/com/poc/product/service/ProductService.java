package com.poc.product.service;

import com.poc.product.domain.Product;
import com.poc.product.repository.ProductRepository;
import com.poc.product.web.ProductDtos.ProductRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Business layer. @Service makes it a bean; the repository is injected through
 * the constructor (Dependency Injection, no "new" anywhere).
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll(boolean inStockOnly) {
        return inStockOnly ? repository.findByStockGreaterThan(0) : repository.findAll();
    }

    public Product findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
    }

    @Transactional
    public Product create(ProductRequest req) {
        repository.findByNameIgnoreCase(req.name()).ifPresent(p -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product '" + req.name() + "' already exists");
        });
        return repository.save(new Product(req.name(), req.price(), req.stock()));
    }

    @Transactional
    public Product update(Long id, ProductRequest req) {
        Product p = findById(id);
        p.setName(req.name());
        p.setPrice(req.price());
        p.setStock(req.stock());
        return p;   // dirty checking: JPA flushes the change on commit
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    @Transactional
    public Product decreaseStock(Long id, int quantity) {
        Product p = findById(id);
        try {
            p.decreaseStock(quantity);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return p;
    }
}
