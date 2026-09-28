package com.trape.backend.payment.service;

import com.trape.backend.common.exception.ApiException;
import com.trape.backend.order.entity.Order;
import com.trape.backend.order.repository.OrderRepository;
import com.trape.backend.order.service.OrderService;
import com.trape.backend.payment.dto.PaymentDtos.VerifyPaymentRequest;
import com.trape.backend.payment.dto.PaymentDtos.VerifyPaymentResponse;
import com.trape.backend.payment.entity.Payment;
import com.trape.backend.payment.repository.PaymentRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two independent paths confirm a payment, matching
 * diagrams/05-payment-race-sequence.mmd:
 *
 *  1. CLIENT CALLBACK — Razorpay Checkout.js calls the frontend's success
 *     handler with order/payment/signature; the frontend POSTs those to
 *     {@link #verifyClientPayment}, which checks the signature itself.
 *  2. WEBHOOK — Razorpay's servers POST the event directly to
 *     {@link #handleWebhookPayload} asynchronously, authenticated by the
 *     X-Razorpay-Signature header over the raw body (webhook secret, not
 *     the API key secret).
 *
 * Both converge on OrderService#markPaid, which is idempotent — whichever
 * arrives first wins, the second is a no-op.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RazorpayService razorpayService;
    private final OrderService orderService;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
                           RazorpayService razorpayService, OrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.razorpayService = razorpayService;
        this.orderService = orderService;
    }

    @Transactional
    public VerifyPaymentResponse verifyClientPayment(VerifyPaymentRequest request) {
        boolean valid = razorpayService.verifyPaymentSignature(
                request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());
        if (!valid) {
            throw ApiException.unprocessable("SIGNATURE_INVALID", "Payment signature verification failed");
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> ApiException.notFound("Payment"));
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        payment.setStatus("CAPTURED");
        paymentRepository.save(payment);

        orderService.markPaid(payment.getOrderId());
        Order order = orderRepository.findById(payment.getOrderId()).orElseThrow(() -> ApiException.notFound("Order"));
        return new VerifyPaymentResponse(true, order.getStatus(), order.getOrderNumber());
    }

    /**
     * Processes a Razorpay webhook event. Caller (PaymentController) has
     * already verified the X-Razorpay-Signature header before calling this.
     */
    @Transactional
    public void handleWebhookPayload(String rawBody) {
        JSONObject payload = new JSONObject(rawBody);
        String event = payload.optString("event", "");
        log.info("Received Razorpay webhook event: {}", event);

        if (!event.startsWith("payment.")) {
            return; // ignore events we don't act on (refund.*, order.paid duplicate, etc.)
        }

        JSONObject paymentEntity = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String razorpayOrderId = paymentEntity.optString("order_id", null);
        String razorpayPaymentId = paymentEntity.optString("id", null);
        if (razorpayOrderId == null) return;

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) {
            log.warn("Webhook referenced unknown Razorpay order {}", razorpayOrderId);
            return;
        }

        switch (event) {
            case "payment.captured" -> {
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setStatus("CAPTURED");
                paymentRepository.save(payment);
                orderService.markPaid(payment.getOrderId());
            }
            case "payment.failed" -> {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
                orderService.markFailed(payment.getOrderId(), "Payment failed at gateway");
            }
            default -> log.debug("Unhandled webhook event type: {}", event);
        }
    }
}
