package com.trape.backend.order.service;

import com.trape.backend.cart.entity.Cart;
import com.trape.backend.cart.entity.CartItem;
import com.trape.backend.cart.repository.CartItemRepository;
import com.trape.backend.cart.repository.CartRepository;
import com.trape.backend.catalog.entity.Product;
import com.trape.backend.catalog.entity.ProductVariant;
import com.trape.backend.catalog.repository.ProductRepository;
import com.trape.backend.catalog.repository.ProductVariantRepository;
import com.trape.backend.auth.entity.User;
import com.trape.backend.auth.repository.UserRepository;
import com.trape.backend.common.dto.PageResponse;
import com.trape.backend.common.exception.ApiException;
import com.trape.backend.common.service.EmailService;
import com.trape.backend.inventory.service.InventoryService;
import com.trape.backend.order.dto.OrderDtos.*;
import com.trape.backend.order.entity.Order;
import com.trape.backend.order.entity.OrderItem;
import com.trape.backend.order.entity.OrderStatusHistory;
import com.trape.backend.order.repository.OrderItemRepository;
import com.trape.backend.order.repository.OrderRepository;
import com.trape.backend.order.repository.OrderStatusHistoryRepository;
import com.trape.backend.payment.entity.Payment;
import com.trape.backend.payment.repository.PaymentRepository;
import com.trape.backend.payment.service.RazorpayService;
import com.trape.backend.promotion.entity.Coupon;
import com.trape.backend.promotion.service.CouponService;
import com.trape.backend.user.entity.Address;
import com.trape.backend.user.repository.AddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Owns the checkout critical section and the order lifecycle. Design notes:
 *
 * - Stock is RESERVED (quantity_reserved++) at checkout, inside the same
 *   transaction as order creation, under a per-variant row lock
 *   (InventoryService.reserveStock). It is only COMMITTED (actually
 *   decremented from quantity_available) once payment is confirmed PAID.
 * - The cart is intentionally NOT cleared at checkout — only on PAID
 *   confirmation. This means a second checkout attempt on the same cart
 *   before the first payment completes will fail fast on insufficient
 *   stock, which is the intended fail-safe against accidental duplicate
 *   orders from a double-click or a second browser tab.
 * - A scheduled sweep cancels PAYMENT_PENDING orders older than the
 *   configured timeout and releases their stock holds (see
 *   releaseStaleReservations).
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(1999);
    private static final BigDecimal STANDARD_SHIPPING_FEE = BigDecimal.valueOf(99);
    private static final long PAYMENT_PENDING_TIMEOUT_MINUTES = 15;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public OrderService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                         ProductVariantRepository variantRepository, ProductRepository productRepository,
                         AddressRepository addressRepository, InventoryService inventoryService,
                         CouponService couponService, OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository, OrderStatusHistoryRepository historyRepository,
                         PaymentRepository paymentRepository, RazorpayService razorpayService,
                         UserRepository userRepository, EmailService emailService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.inventoryService = inventoryService;
        this.couponService = couponService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.historyRepository = historyRepository;
        this.paymentRepository = paymentRepository;
        this.razorpayService = razorpayService;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public CheckoutResponse checkout(UUID userId, CheckoutRequest request) {
        Address address = addressRepository.findById(request.shippingAddressId())
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Shipping address"));

        Cart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> ApiException.badRequest("EMPTY_CART", "Your cart is empty"));
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            throw ApiException.badRequest("EMPTY_CART", "Your cart is empty");
        }

        Map<UUID, ProductVariant> variantsById = variantRepository.findAllById(
                items.stream().map(CartItem::getVariantId).toList()
        ).stream().collect(Collectors.toMap(ProductVariant::getId, v -> v));
        Map<UUID, Product> productsById = productRepository.findAllById(
                variantsById.values().stream().map(ProductVariant::getProductId).distinct().toList()
        ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        // 1. Reserve stock for every line — any shortfall aborts the whole transaction.
        for (CartItem item : items) {
            inventoryService.reserveStock(item.getVariantId(), item.getQuantity());
        }

        // 2. Pricing
        BigDecimal subtotal = items.stream()
                .map(i -> i.getPriceSnapshot().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        Coupon coupon = null;
        if (cart.getCouponId() != null) {
            coupon = couponService.validateForCartById(cart.getCouponId(), subtotal);
            discount = coupon.computeDiscount(subtotal);
        }

        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal shippingFee = afterDiscount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : STANDARD_SHIPPING_FEE;
        BigDecimal total = afterDiscount.add(shippingFee);

        // 3. Create order + items
        String orderNumber = generateOrderNumber();
        Order order = new Order(orderNumber, userId, address.getId(),
                coupon == null ? null : coupon.getId(), subtotal, discount, shippingFee, total);
        order = orderRepository.save(order);
        recordHistory(order.getId(), "CREATED", "Order created from cart", userId);

        for (CartItem item : items) {
            ProductVariant variant = variantsById.get(item.getVariantId());
            Product product = variant == null ? null : productsById.get(variant.getProductId());
            String label = variant == null ? null : joinNonNull(variant.getSize(), variant.getColor());
            orderItemRepository.save(new OrderItem(order.getId(), item.getVariantId(),
                    product == null ? "Unknown product" : product.getName(), label,
                    item.getQuantity(), item.getPriceSnapshot()));
        }

        // 4. Move to PAYMENT_PENDING and open a Razorpay order
        OrderStateMachine.assertTransitionAllowed(order.getStatus(), "PAYMENT_PENDING");
        order.setStatus("PAYMENT_PENDING");
        order = orderRepository.save(order);
        recordHistory(order.getId(), "PAYMENT_PENDING", "Awaiting payment", userId);

        String razorpayOrderId = razorpayService.createOrder(total, orderNumber);
        paymentRepository.save(new Payment(order.getId(), razorpayOrderId, total));

        return new CheckoutResponse(
                toResponse(order),
                razorpayOrderId,
                razorpayService.getKeyId(),
                RazorpayService.toPaise(total),
                "INR"
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listForUser(UUID userId, int page, int pageSize) {
        Page<Order> result = orderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, Math.min(pageSize, 100)));
        return PageResponse.from(result, result.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getForUser(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("Order"));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelForUser(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("Order"));
        return cancel(order, "Cancelled by customer", userId);
    }

    // --- Admin operations ---------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listAllForAdmin(String status, int page, int pageSize) {
        Page<Order> result = (status == null || status.isBlank())
                ? orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(pageSize, 100)))
                : orderRepository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, Math.min(pageSize, 100)));
        return PageResponse.from(result, result.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional
    public OrderResponse updateStatusAsAdmin(UUID orderId, UpdateOrderStatusRequest request, UUID adminUserId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        String target = request.status().toUpperCase();

        if ("CANCELLED".equals(target)) {
            return cancel(order, request.note(), adminUserId);
        }

        OrderStateMachine.assertTransitionAllowed(order.getStatus(), target);
        order.setStatus(target);
        orderRepository.save(order);
        recordHistory(order.getId(), target, request.note(), adminUserId);
        return toResponse(order);
    }

    // --- Internal helpers used by PaymentService -----------------------------

    @Transactional
    public void markPaid(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if ("PAID".equals(order.getStatus())) return; // idempotent — webhook + client callback can both fire
        OrderStateMachine.assertTransitionAllowed(order.getStatus(), "PAID");

        for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
            inventoryService.commitReservation(item.getVariantId(), item.getQuantity());
        }
        order.setStatus("PAID");
        orderRepository.save(order);
        recordHistory(order.getId(), "PAID", "Payment confirmed", null);

        if (order.getCouponId() != null) {
            couponService.markUsed(order.getCouponId());
        }

        cartRepository.findByUserIdAndStatus(order.getUserId(), "ACTIVE").ifPresent(cart -> {
            cartItemRepository.deleteByCartId(cart.getId());
            cart.setCouponId(null);
            cartRepository.save(cart);
        });

        // Send order confirmation email asynchronously
        final Order confirmedOrder = order;
        userRepository.findById(confirmedOrder.getUserId()).ifPresent(user -> {
            try {
                emailService.sendOrderConfirmation(user.getEmail(), user.getFullName(), confirmedOrder.getOrderNumber(), confirmedOrder.getTotal());
            } catch (Exception e) {
                log.error("Failed to send order confirmation email for order {}: {}", confirmedOrder.getOrderNumber(), e.getMessage());
            }
        });
    }

    @Transactional
    public void markFailed(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (OrderStateMachine.isTerminal(order.getStatus())) return;
        OrderStateMachine.assertTransitionAllowed(order.getStatus(), "FAILED");
        releaseAllItems(order);
        order.setStatus("FAILED");
        orderRepository.save(order);
        recordHistory(order.getId(), "FAILED", reason, null);
    }

    /** Sweeps PAYMENT_PENDING orders past the timeout, releasing their stock holds. Runs every 5 minutes. */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void releaseStaleReservations() {
        Instant cutoff = Instant.now().minus(PAYMENT_PENDING_TIMEOUT_MINUTES, ChronoUnit.MINUTES);
        List<Order> stale = orderRepository.findByStatusAndCreatedAtBefore("PAYMENT_PENDING", cutoff);
        for (Order order : stale) {
            try {
                releaseAllItems(order);
                order.setStatus("CANCELLED");
                orderRepository.save(order);
                recordHistory(order.getId(), "CANCELLED", "Auto-cancelled: payment not completed in time", null);
                log.info("Auto-cancelled stale order {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to auto-cancel stale order {}", order.getOrderNumber(), e);
            }
        }
    }

    // --- Private helpers ------------------------------------------------------

    private OrderResponse cancel(Order order, String note, UUID actorId) {
        OrderStateMachine.assertTransitionAllowed(order.getStatus(), "CANCELLED");
        if (!OrderStateMachine.isStockCommitted(order.getStatus())) {
            releaseAllItems(order); // still just a reservation — release it
        }
        // NOTE: if stock was already committed (PAID/PROCESSING), cancelling here
        // does not auto-restock or auto-refund — that's a deliberate manual step
        // for admins (a real refund must go through Razorpay's refund API first).
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        recordHistory(order.getId(), "CANCELLED", note, actorId);
        return toResponse(order);
    }

    private void releaseAllItems(Order order) {
        for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
            inventoryService.releaseReservation(item.getVariantId(), item.getQuantity());
        }
    }

    private void recordHistory(UUID orderId, String status, String note, UUID actorId) {
        historyRepository.save(new OrderStatusHistory(orderId, status, note, actorId));
    }

    private String generateOrderNumber() {
        String candidate;
        int attempts = 0;
        do {
            String datePart = Instant.now().toString().substring(0, 10).replace("-", "");
            String randomPart = String.valueOf((int) (Math.random() * 900_000) + 100_000);
            candidate = "TRP-" + datePart + "-" + randomPart;
            attempts++;
        } while (orderRepository.findByOrderNumber(candidate).isPresent() && attempts < 5);
        return candidate;
    }

    private String joinNonNull(String a, String b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return a + " / " + b;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(i -> new OrderItemResponse(i.getId(), i.getVariantId(), i.getProductNameSnapshot(),
                        i.getVariantLabelSnapshot(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal()))
                .toList();
        List<OrderStatusEvent> history = historyRepository.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(h -> new OrderStatusEvent(h.getStatus(), h.getNote(), h.getCreatedAt()))
                .toList();
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getStatus(),
                order.getSubtotal(), order.getDiscount(), order.getShippingFee(), order.getTotal(),
                items, history, order.getCreatedAt());
    }
}
