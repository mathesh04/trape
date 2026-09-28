package com.trape.backend.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.trape.backend.common.exception.ApiException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Thin wrapper around the Razorpay Java SDK. Every rupee amount crossing
 * this boundary is converted to integer paise (Razorpay's unit) — see
 * toPaise/fromPaise — to avoid floating point drift.
 */
@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final RazorpayClient client;

    public RazorpayService(@Value("${razorpay.key-id}") String keyId,
                            @Value("${razorpay.key-secret}") String keySecret,
                            @Value("${razorpay.webhook-secret}") String webhookSecret) throws RazorpayException {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.client = new RazorpayClient(keyId, keySecret);
    }

    public String getKeyId() {
        return keyId;
    }

    /** Creates a Razorpay order for the given amount and returns its id (order_XXXX). */
    public String createOrder(BigDecimal amountInRupees, String receiptId) {
        try {
            JSONObject request = new JSONObject();
            request.put("amount", toPaise(amountInRupees));
            request.put("currency", "INR");
            request.put("receipt", receiptId);
            request.put("payment_capture", 1); // auto-capture on successful auth
            Order order = client.orders.create(request);
            return order.get("id");
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for receipt {}", receiptId, e);
            throw ApiException.badRequest("PAYMENT_GATEWAY_ERROR", "Could not initiate payment. Please try again.");
        }
    }

    /** Verifies the signature Razorpay Checkout.js returns to the frontend on success (HMAC-SHA256). */
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            log.warn("Payment signature verification failed for order {}", razorpayOrderId, e);
            return false;
        }
    }

    /**
     * Verifies the X-Razorpay-Signature header on incoming webhook calls —
     * HMAC-SHA256 of the raw request body using the webhook secret
     * (separate from the API key secret; configured in the Razorpay dashboard).
     */
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (signatureHeader == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equals(signatureHeader);
        } catch (Exception e) {
            log.error("Webhook signature verification error", e);
            return false;
        }
    }

    public static long toPaise(BigDecimal rupees) {
        return rupees.movePointRight(2).longValueExact();
    }
}
