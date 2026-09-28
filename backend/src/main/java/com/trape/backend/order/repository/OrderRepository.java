package com.trape.backend.order.repository;

import com.trape.backend.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Order> findByStatusAndCreatedAtBefore(String status, Instant cutoff);
}
