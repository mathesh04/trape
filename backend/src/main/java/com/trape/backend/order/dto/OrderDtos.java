package com.trape.backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderDtos {

    public record CheckoutRequest(
            @NotNull UUID shippingAddressId
    ) {
    }

    public record OrderItemResponse(
            UUID id, UUID variantId, String productName, String variantLabel,
            int quantity, BigDecimal unitPrice, BigDecimal lineTotal
    ) {
    }

    public record OrderStatusEvent(String status, String note, Instant at) {
    }

    public record OrderResponse(
            UUID id, String orderNumber, String status,
            BigDecimal subtotal, BigDecimal discount, BigDecimal shippingFee, BigDecimal total,
            List<OrderItemResponse> items, List<OrderStatusEvent> history,
            Instant createdAt
    ) {
    }

    /** Returned right after checkout — everything the frontend needs to open Razorpay Checkout.js. */
    public record CheckoutResponse(
            OrderResponse order,
            String razorpayOrderId,
            String razorpayKeyId,
            long amountInPaise,
            String currency
    ) {
    }

    public record UpdateOrderStatusRequest(
            @NotBlank String status,
            String note
    ) {
    }
}
