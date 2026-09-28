package com.trape.backend.order.service;

import com.trape.backend.common.exception.ApiException;

import java.util.Map;
import java.util.Set;

/**
 * Encodes the order lifecycle from diagrams/04-order-state-machine.mmd.
 * Every status change in OrderService must go through
 * {@link #assertTransitionAllowed} so an invalid jump (e.g. SHIPPED ->
 * PAYMENT_PENDING) fails loudly instead of silently corrupting state.
 */
public final class OrderStateMachine {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "CREATED", Set.of("PAYMENT_PENDING", "CANCELLED"),
            "PAYMENT_PENDING", Set.of("PAID", "FAILED", "CANCELLED"),
            "PAID", Set.of("PROCESSING", "CANCELLED", "REFUNDED"),
            "PROCESSING", Set.of("SHIPPED", "CANCELLED", "REFUNDED"),
            "SHIPPED", Set.of("DELIVERED", "REFUNDED"),
            "DELIVERED", Set.of("REFUNDED"),
            "CANCELLED", Set.of(),
            "REFUNDED", Set.of(),
            "FAILED", Set.of("PAYMENT_PENDING", "CANCELLED")
    );

    private OrderStateMachine() {
    }

    public static void assertTransitionAllowed(String from, String to) {
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw ApiException.conflict("INVALID_STATE_TRANSITION",
                    "Cannot move order from " + from + " to " + to);
        }
    }

    /** Statuses at/after which stock has been permanently committed (not just reserved). */
    public static boolean isStockCommitted(String status) {
        return Set.of("PAID", "PROCESSING", "SHIPPED", "DELIVERED").contains(status);
    }

    public static boolean isTerminal(String status) {
        return Set.of("CANCELLED", "REFUNDED", "DELIVERED").contains(status);
    }
}
