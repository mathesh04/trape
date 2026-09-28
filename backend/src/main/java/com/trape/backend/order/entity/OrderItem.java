package com.trape.backend.order.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Column(name = "variant_label_snapshot")
    private String variantLabelSnapshot;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    protected OrderItem() {
    }

    public OrderItem(UUID orderId, UUID variantId, String productNameSnapshot, String variantLabelSnapshot,
                      int quantity, BigDecimal unitPrice) {
        this.orderId = orderId;
        this.variantId = variantId;
        this.productNameSnapshot = productNameSnapshot;
        this.variantLabelSnapshot = variantLabelSnapshot;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getVariantId() { return variantId; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public String getVariantLabelSnapshot() { return variantLabelSnapshot; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
