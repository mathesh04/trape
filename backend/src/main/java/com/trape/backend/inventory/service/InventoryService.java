package com.trape.backend.inventory.service;

import com.trape.backend.common.exception.ApiException;
import com.trape.backend.inventory.entity.Inventory;
import com.trape.backend.inventory.exception.OutOfStockException;
import com.trape.backend.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Owns all stock mutation. Reservation happens at checkout time (not
 * add-to-cart): quantity_reserved is bumped inside the same transaction as
 * order creation, using SELECT ... FOR UPDATE on the variant's inventory
 * row (see InventoryRepository#findByVariantIdForUpdate). The reservation is
 * released back (on cancel/timeout) or "committed" — i.e. actually
 * decremented from quantity_available — only once the order reaches PAID.
 */
@Service
public class InventoryService {

    private static final String DEFAULT_WAREHOUSE = "DEFAULT";

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /** Read-only stock check for PDP/PLP display — no lock. */
    @Transactional(readOnly = true)
    public Map<UUID, Integer> sellableQuantitiesByVariant(List<UUID> variantIds) {
        return inventoryRepository.findByVariantIdIn(variantIds).stream()
                .collect(Collectors.toMap(Inventory::getVariantId, Inventory::getSellableQuantity, Integer::sum));
    }

    /**
     * Reserves {@code qty} units of a variant. Throws OutOfStockException if
     * not enough sellable stock remains. Must be called within the caller's
     * existing transaction (checkout) so the lock is held for the whole
     * order-creation critical section.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveStock(UUID variantId, int qty) {
        Inventory inv = inventoryRepository.findByVariantIdForUpdate(variantId, DEFAULT_WAREHOUSE)
                .orElseThrow(() -> new OutOfStockException(variantId));
        if (inv.getSellableQuantity() < qty) {
            throw new OutOfStockException(variantId);
        }
        inv.setQuantityReserved(inv.getQuantityReserved() + qty);
        inventoryRepository.save(inv);
    }

    /** Releases a previously-held reservation (order cancelled/timed out before payment). */
    @Transactional
    public void releaseReservation(UUID variantId, int qty) {
        inventoryRepository.findByVariantIdForUpdate(variantId, DEFAULT_WAREHOUSE).ifPresent(inv -> {
            inv.setQuantityReserved(Math.max(0, inv.getQuantityReserved() - qty));
            inventoryRepository.save(inv);
        });
    }

    /** Commits a reservation into a real decrement — called when payment is confirmed (order -> PAID). */
    @Transactional
    public void commitReservation(UUID variantId, int qty) {
        inventoryRepository.findByVariantIdForUpdate(variantId, DEFAULT_WAREHOUSE).ifPresent(inv -> {
            inv.setQuantityReserved(Math.max(0, inv.getQuantityReserved() - qty));
            inv.setQuantityAvailable(Math.max(0, inv.getQuantityAvailable() - qty));
            inventoryRepository.save(inv);
        });
    }

    /** Admin: set absolute stock level for a variant. */
    @Transactional
    public void setStock(UUID variantId, int quantityAvailable) {
        Inventory inv = inventoryRepository.findByVariantIdAndWarehouseId(variantId, DEFAULT_WAREHOUSE)
                .orElseGet(() -> new Inventory(variantId, DEFAULT_WAREHOUSE, 0));
        if (quantityAvailable < 0) {
            throw ApiException.badRequest("INVALID_QUANTITY", "Quantity cannot be negative");
        }
        inv.setQuantityAvailable(quantityAvailable);
        inventoryRepository.save(inv);
    }
}
