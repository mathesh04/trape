package com.trape.backend.cart.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();

    protected CartItem() {
    }

    public CartItem(UUID cartId, UUID variantId, int quantity, BigDecimal priceSnapshot) {
        this.cartId = cartId;
        this.variantId = variantId;
        this.quantity = quantity;
        this.priceSnapshot = priceSnapshot;
    }

    public UUID getId() { return id; }
    public UUID getCartId() { return cartId; }
    public UUID getVariantId() { return variantId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getPriceSnapshot() { return priceSnapshot; }
    public void setPriceSnapshot(BigDecimal priceSnapshot) { this.priceSnapshot = priceSnapshot; }
}
