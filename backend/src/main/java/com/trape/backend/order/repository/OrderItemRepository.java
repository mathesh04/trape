package com.trape.backend.order.repository;

import com.trape.backend.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Used by ReviewService to enforce "verified purchase" reviews: a user
     * may only review a product they have an item for in a DELIVERED order.
     */
    @Query("""
            select count(oi) > 0 from OrderItem oi
              join com.trape.backend.order.entity.Order o on o.id = oi.orderId
              join com.trape.backend.catalog.entity.ProductVariant v on v.id = oi.variantId
            where o.userId = :userId and v.productId = :productId and o.status = 'DELIVERED'
            """)
    boolean existsDeliveredPurchaseByUserAndProduct(@Param("userId") UUID userId, @Param("productId") UUID productId);
}
