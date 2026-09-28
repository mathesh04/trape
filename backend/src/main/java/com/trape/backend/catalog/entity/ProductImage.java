package com.trape.backend.catalog.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String url;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    protected ProductImage() {
    }

    public ProductImage(UUID productId, String url, String altText, int displayOrder) {
        this.productId = productId;
        this.url = url;
        this.altText = altText;
        this.displayOrder = displayOrder;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAltText() {
        return altText;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
