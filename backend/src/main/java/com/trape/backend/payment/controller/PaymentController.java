package com.trape.backend.payment.controller;

import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.payment.dto.PaymentDtos.VerifyPaymentRequest;
import com.trape.backend.payment.dto.PaymentDtos.VerifyPaymentResponse;
import com.trape.backend.payment.service.PaymentService;
import com.trape.backend.payment.service.RazorpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;

    public PaymentController(PaymentService paymentService, RazorpayService razorpayService) {
        this.paymentService = paymentService;
        this.razorpayService = razorpayService;
    }

    /** Called by the frontend right after Razorpay Checkout.js's success handler fires. */
    @PostMapping("/verify")
    public ApiResponse<VerifyPaymentResponse> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        return ApiResponse.ok(paymentService.verifyClientPayment(request));
    }

    /**
     * Razorpay server-to-server webhook — NOT authenticated via JWT (see
     * SecurityConfig, permitAll for this path). Authenticity instead comes
     * from the HMAC signature over the raw body, verified here before any
     * parsing happens.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(HttpServletRequest request,
                                         @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) throws IOException {
        String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        if (!razorpayService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Rejected webhook call with invalid signature");
            return ResponseEntity.status(401).build();
        }

        paymentService.handleWebhookPayload(rawBody);
        return ResponseEntity.ok().build();
    }
}
