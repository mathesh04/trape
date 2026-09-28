package com.trape.backend.cart.controller;

import com.trape.backend.cart.dto.CartDtos.*;
import com.trape.backend.cart.service.CartService;
import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.promotion.dto.CouponDtos.ApplyCouponRequest;
import com.trape.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartResponse> view(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestHeader(value = "X-Guest-Token", required = false) String guestToken) {
        return ApiResponse.ok(cartService.view(userId(principal), guestToken));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                              @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
                                              @Valid @RequestBody AddItemRequest request) {
        return ApiResponse.ok(cartService.addItem(userId(principal), guestToken, request));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(@AuthenticationPrincipal UserPrincipal principal,
                                                 @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
                                                 @PathVariable UUID itemId,
                                                 @Valid @RequestBody UpdateItemRequest request) {
        return ApiResponse.ok(cartService.updateItem(userId(principal), guestToken, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                                 @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
                                                 @PathVariable UUID itemId) {
        return ApiResponse.ok(cartService.removeItem(userId(principal), guestToken, itemId));
    }

    @PostMapping("/coupon")
    public ApiResponse<CartResponse> applyCoupon(@AuthenticationPrincipal UserPrincipal principal,
                                                  @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
                                                  @Valid @RequestBody ApplyCouponRequest request) {
        return ApiResponse.ok(cartService.applyCoupon(userId(principal), guestToken, request.code()));
    }

    @DeleteMapping("/coupon")
    public ApiResponse<CartResponse> removeCoupon(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestHeader(value = "X-Guest-Token", required = false) String guestToken) {
        return ApiResponse.ok(cartService.removeCoupon(userId(principal), guestToken));
    }

    private UUID userId(UserPrincipal principal) {
        return principal == null ? null : principal.getId();
    }
}
