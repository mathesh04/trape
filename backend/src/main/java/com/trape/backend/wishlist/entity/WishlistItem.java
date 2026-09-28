package com.trape.backend.wishlist.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wishlist_items")
public class WishlistItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected WishlistItem() {
    }

    public WishlistItem(UUID userId, UUID productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getProductId() { return productId; }
}
