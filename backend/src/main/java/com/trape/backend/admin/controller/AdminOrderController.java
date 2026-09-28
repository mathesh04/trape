package com.trape.backend.admin.controller;

import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.common.dto.PageResponse;
import com.trape.backend.order.dto.OrderDtos.OrderResponse;
import com.trape.backend.order.dto.OrderDtos.UpdateOrderStatusRequest;
import com.trape.backend.order.service.OrderService;
import com.trape.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(@RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(orderService.listAllForAdmin(status, page, pageSize));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable UUID id,
                                                     @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.ok(orderService.updateStatusAsAdmin(id, request, principal.getId()));
    }
}
