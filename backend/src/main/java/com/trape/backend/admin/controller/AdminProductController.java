package com.trape.backend.admin.controller;

import com.trape.backend.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.trape.backend.catalog.dto.CatalogDtos.ProductWriteRequest;
import com.trape.backend.catalog.service.ProductService;
import com.trape.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ApiResponse<ProductDetailResponse> create(@Valid @RequestBody ProductWriteRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDetailResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductWriteRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> archive(@PathVariable UUID id) {
        productService.archive(id);
        return ApiResponse.ok(null);
    }
}
