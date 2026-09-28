package com.trape.backend.catalog.controller;

import com.trape.backend.catalog.dto.CatalogDtos.CreateReviewRequest;
import com.trape.backend.catalog.dto.CatalogDtos.ReviewResponse;
import com.trape.backend.catalog.service.ReviewService;
import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.common.dto.PageResponse;
import com.trape.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewResponse>> list(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(reviewService.listByProduct(productId, page, pageSize));
    }

    @PostMapping
    public ApiResponse<ReviewResponse> add(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.ok(reviewService.addReview(productId, principal.getId(), request));
    }
}
