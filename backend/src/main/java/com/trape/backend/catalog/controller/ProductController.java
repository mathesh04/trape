package com.trape.backend.catalog.controller;

import com.trape.backend.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.trape.backend.catalog.dto.CatalogDtos.ProductSummaryResponse;
import com.trape.backend.catalog.service.ProductService;
import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.common.dto.PageResponse;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductSummaryResponse>> list(
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(productService.search(category, q, size, color, minPrice, maxPrice, sort, page, pageSize));
    }

    @GetMapping("/{slug}")
    public ApiResponse<ProductDetailResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.ok(productService.getBySlug(slug));
    }
}
