package com.poc.product.repository;

import com.poc.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA: no implementation needed. Spring generates it at runtime,
 * including the SQL for the derived query methods below.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByNameIgnoreCase(String name);

    List<Product> findByStockGreaterThan(int stock);
}
