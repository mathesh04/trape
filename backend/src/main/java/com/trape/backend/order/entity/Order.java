package com.trape.backend.order.entity;

import com.trape.backend.common.entity.Auditable;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "shipping_address_id", nullable = false)
    private UUID shippingAddressId;

    @Column(name = "coupon_id")
    private UUID couponId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private String status = "CREATED";

    protected Order() {
    }

    public Order(String orderNumber, UUID userId, UUID shippingAddressId, UUID couponId,
                 BigDecimal subtotal, BigDecimal discount, BigDecimal shippingFee, BigDecimal total) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.shippingAddressId = shippingAddressId;
        this.couponId = couponId;
        this.subtotal = subtotal;
        this.discount = discount;
        this.shippingFee = shippingFee;
        this.total = total;
    }

    public UUID getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public UUID getUserId() { return userId; }
    public UUID getShippingAddressId() { return shippingAddressId; }
    public UUID getCouponId() { return couponId; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
