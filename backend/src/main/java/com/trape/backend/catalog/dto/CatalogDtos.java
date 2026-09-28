package com.trape.backend.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CatalogDtos {

    public record CategoryResponse(
            UUID id, UUID parentId, String name, String slug, int displayOrder
    ) {
    }

    public record VariantResponse(
            UUID id, String sku, String size, String color,
            BigDecimal price, BigDecimal mrp, boolean inStock, int availableQuantity
    ) {
    }

    public record ProductImageResponse(UUID id, String url, String altText, int displayOrder) {
    }

    public record ProductSummaryResponse(
            UUID id, String name, String slug, String brand,
            BigDecimal basePrice, String primaryImageUrl, Double avgRating, long reviewCount,
            BigDecimal mrp
    ) {
    }

    public record ProductDetailResponse(
            UUID id, String name, String slug, String description, String brand,
            BigDecimal basePrice, String status, UUID categoryId,
            List<ProductImageResponse> images, List<VariantResponse> variants,
            Double avgRating, long reviewCount
    ) {
    }

    public record ReviewResponse(
            UUID id, UUID userId, String userName, int rating, String comment, Instant createdAt
    ) {
    }

    public record CreateReviewRequest(
            @Min(1) @Max(5) int rating,
            String comment
    ) {
    }


    public record VariantWriteRequest(
            UUID id, 
            @NotBlank String sku,
            String size, String color,
            BigDecimal price, BigDecimal mrp,
            Integer initialStock
    ) {
    }

    public record ProductWriteRequest(
            @NotBlank String name,
            String slug,
            String description,
            String brand,
            BigDecimal basePrice,
            UUID categoryId,
            String status,
            List<String> imageUrls,
            List<VariantWriteRequest> variants
    ) {
    }

    public record InventoryUpdateRequest(
            @NotBlank String variantSkuOrId,
            int quantityAvailable
    ) {
    }

    public record CategoryWriteRequest(
            @NotBlank String name,
            String slug,
            UUID parentId,
            int displayOrder
    ) {
    }
}
