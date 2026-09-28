package com.trape.backend.wishlist.controller;

import com.trape.backend.catalog.dto.CatalogDtos.ProductSummaryResponse;
import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.security.UserPrincipal;
import com.trape.backend.wishlist.service.WishlistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ApiResponse<List<ProductSummaryResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(wishlistService.list(principal.getId()));
    }

    @PutMapping("/{productId}")
    public ApiResponse<Void> add(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID productId) {
        wishlistService.add(principal.getId(), productId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID productId) {
        wishlistService.remove(principal.getId(), productId);
        return ApiResponse.ok(null);
    }
}
