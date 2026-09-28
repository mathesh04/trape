package com.trape.backend.catalog.entity;

import com.trape.backend.common.entity.Auditable;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    private String brand = "Trape";

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private String status = "DRAFT"; // DRAFT | ACTIVE | ARCHIVED

    // Note: images/variants are intentionally NOT mapped as JPA relations here.
    // They're fetched explicitly via ProductImageRepository / ProductVariantRepository
    // in the service layer — keeps fetch strategy explicit (no N+1 surprises) and
    // keeps this entity a simple row mapping to `products`.

    protected Product() {
    }

    public Product(UUID categoryId, String name, String slug, String description, String brand, BigDecimal basePrice) {
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.brand = brand;
        this.basePrice = basePrice;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
