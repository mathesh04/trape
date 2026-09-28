package com.trape.backend.order.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String status;

    private String note;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected OrderStatusHistory() {
    }

    public OrderStatusHistory(UUID orderId, String status, String note, UUID createdBy) {
        this.orderId = orderId;
        this.status = status;
        this.note = note;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
