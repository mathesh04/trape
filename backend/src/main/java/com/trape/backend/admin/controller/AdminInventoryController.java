package com.trape.backend.admin.controller;

import com.trape.backend.catalog.repository.ProductVariantRepository;
import com.trape.backend.common.dto.ApiResponse;
import com.trape.backend.common.exception.ApiException;
import com.trape.backend.inventory.service.InventoryService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final ProductVariantRepository variantRepository;

    public AdminInventoryController(InventoryService inventoryService, ProductVariantRepository variantRepository) {
        this.inventoryService = inventoryService;
        this.variantRepository = variantRepository;
    }

    public record SetStockRequest(@NotNull UUID variantId, @Min(0) int quantityAvailable) {
    }

    @PutMapping
    public ApiResponse<Void> setStock(@RequestBody SetStockRequest request) {
        if (!variantRepository.existsById(request.variantId())) {
            throw ApiException.notFound("Variant");
        }
        inventoryService.setStock(request.variantId(), request.quantityAvailable());
        return ApiResponse.ok(null);
    }
}
