package com.poc.product.config;

import com.poc.product.domain.Product;
import com.poc.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/** @Configuration + @Bean: Java-based bean definition. Seeds demo data on startup. */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Product("Laptop", new BigDecimal("25000.00"), 10));
                repository.save(new Product("Mouse", new BigDecimal("350.00"), 100));
                repository.save(new Product("Keyboard", new BigDecimal("900.00"), 50));
            }
        };
    }
}
