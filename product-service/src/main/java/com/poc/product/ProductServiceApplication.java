package com.poc.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point. @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan,
 * so the IoC container scans com.poc.product.* and creates/wires all the beans.
 */
@SpringBootApplication
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
