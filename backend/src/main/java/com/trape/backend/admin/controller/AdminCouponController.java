package com.trape.backend.admin.controller;

import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.promotion.dto.CouponDtos.CouponResponse;
import com.trape.backend.promotion.dto.CouponDtos.CouponWriteRequest;
import com.trape.backend.promotion.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/coupons")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public ApiResponse<List<CouponResponse>> list() {
        return ApiResponse.ok(couponService.listAll());
    }

    @PostMapping
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponWriteRequest request) {
        return ApiResponse.ok(couponService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        couponService.deactivate(id);
        return ApiResponse.ok(null);
    }
}
