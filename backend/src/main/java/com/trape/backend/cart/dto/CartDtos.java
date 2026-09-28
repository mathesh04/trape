package com.trape.backend.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CartDtos {

    public record AddItemRequest(
            @NotNull UUID variantId,
            @Min(1) int quantity
    ) {
    }

    public record UpdateItemRequest(
            @Min(1) int quantity
    ) {
    }

    public record CartItemResponse(
            UUID id, UUID variantId, String productName, String slug, String imageUrl,
            String size, String color, int quantity, BigDecimal unitPrice, BigDecimal lineTotal,
            boolean inStock, int availableQuantity
    ) {
    }

    public record CartResponse(
            UUID id, List<CartItemResponse> items,
            BigDecimal subtotal, BigDecimal discount, String couponCode, BigDecimal total,
            String guestToken
    ) {
    }
}
