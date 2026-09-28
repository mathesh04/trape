package com.trape.backend.inventory.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId = "DEFAULT";

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable = 0;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private long version; // optimistic-lock safety net alongside the pessimistic read lock

    protected Inventory() {
    }

    public Inventory(UUID variantId, String warehouseId, int quantityAvailable) {
        this.variantId = variantId;
        this.warehouseId = warehouseId;
        this.quantityAvailable = quantityAvailable;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
        this.updatedAt = Instant.now();
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
        this.updatedAt = Instant.now();
    }

    /** Units that can still be sold right now = available - already reserved. */
    public int getSellableQuantity() {
        return quantityAvailable - quantityReserved;
    }
}
