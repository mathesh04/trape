package com.trape.backend.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class PaymentDtos {

    /** Sent by the frontend after Razorpay Checkout.js's handler fires with a successful payment. */
    public record VerifyPaymentRequest(
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature
    ) {
    }

    public record VerifyPaymentResponse(
            boolean verified,
            String orderStatus,
            String orderNumber
    ) {
    }
}
