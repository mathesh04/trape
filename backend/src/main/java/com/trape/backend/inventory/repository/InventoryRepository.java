package com.trape.backend.inventory.repository;

import com.trape.backend.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByVariantIdAndWarehouseId(UUID variantId, String warehouseId);

    List<Inventory> findByVariantIdIn(List<UUID> variantIds);

    /**
     * Pessimistic row lock for the checkout-time stock reservation critical
     * section (see InventoryService.reserveStock). Held only for the
     * duration of the surrounding @Transactional method — short critical
     * section, safe at D2C scale per the LLD.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.variantId = :variantId and i.warehouseId = :warehouseId")
    Optional<Inventory> findByVariantIdForUpdate(@Param("variantId") UUID variantId, @Param("warehouseId") String warehouseId);
}
