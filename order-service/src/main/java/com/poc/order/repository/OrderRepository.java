package com.poc.order.repository;

import com.poc.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

/** Spring Data JPA repository with a derived query and a JPQL query. */
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUsernameOrderByCreatedAtDesc(String username);

    @Query("select coalesce(sum(o.totalPrice), 0) from Order o where o.username = :username")
    BigDecimal totalSpentBy(String username);
}
