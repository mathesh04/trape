package com.trape.backend.promotion.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "discount_type", nullable = false)
    private String discountType; // PERCENT | FLAT

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_cart_value", precision = 10, scale = 2)
    private BigDecimal minCartValue = BigDecimal.ZERO;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Coupon() {
    }

    public Coupon(String code, String discountType, BigDecimal discountValue, BigDecimal minCartValue,
                  Instant validFrom, Instant validUntil, Integer usageLimit) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minCartValue = minCartValue;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.usageLimit = usageLimit;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMinCartValue() { return minCartValue; }
    public void setMinCartValue(BigDecimal minCartValue) { this.minCartValue = minCartValue; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public int getUsageCount() { return usageCount; }
    public void incrementUsage() { this.usageCount++; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isCurrentlyValid() {
        Instant now = Instant.now();
        boolean withinWindow = !now.isBefore(validFrom) && !now.isAfter(validUntil);
        boolean withinLimit = usageLimit == null || usageCount < usageLimit;
        return active && withinWindow && withinLimit;
    }

    public BigDecimal computeDiscount(BigDecimal cartSubtotal) {
        if (cartSubtotal.compareTo(minCartValue == null ? BigDecimal.ZERO : minCartValue) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = "PERCENT".equals(discountType)
                ? cartSubtotal.multiply(discountValue).divide(BigDecimal.valueOf(100))
                : discountValue;
        return discount.min(cartSubtotal); // never discount more than the subtotal
    }
}
