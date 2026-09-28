package com.trape.backend.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CouponDtos {

    public record CouponResponse(
            UUID id, String code, String discountType, BigDecimal discountValue,
            BigDecimal minCartValue, Instant validFrom, Instant validUntil,
            Integer usageLimit, int usageCount, boolean active
    ) {
    }

    public record CouponWriteRequest(
            @NotBlank String code,
            @NotBlank String discountType,
            @NotNull BigDecimal discountValue,
            BigDecimal minCartValue,
            @NotNull Instant validFrom,
            @NotNull Instant validUntil,
            Integer usageLimit
    ) {
    }

    public record ApplyCouponRequest(@NotBlank String code) {
    }
}
