package com.quickbite.order.repository;

import com.quickbite.order.entity.OrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findByUserEmailAndOrderReference(String userEmail, String orderReference);

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findByOrderReference(String orderReference);

    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findAllByOrderByCreatedAtDesc();
}
