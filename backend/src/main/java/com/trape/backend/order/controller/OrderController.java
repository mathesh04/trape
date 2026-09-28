package com.trape.backend.order.controller;

import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.common.dto.PageResponse;
import com.trape.backend.order.dto.OrderDtos.*;
import com.trape.backend.order.service.OrderService;
import com.trape.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout(@AuthenticationPrincipal UserPrincipal principal,
                                                    @Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.ok(orderService.checkout(principal.getId(), request));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(@AuthenticationPrincipal UserPrincipal principal,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(orderService.listForUser(principal.getId(), page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(orderService.getForUser(principal.getId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(orderService.cancelForUser(principal.getId(), id));
    }
}
