package com.trape.backend.catalog.repository;

import com.trape.backend.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductIdOrderBySizeAsc(UUID productId);

    List<ProductVariant> findByProductIdInAndActiveTrue(List<UUID> productIds);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("""
            select distinct v.productId from ProductVariant v
            where v.active = true
              and (:size is null or v.size = :size)
              and (:color is null or lower(v.color) = lower(:color))
            """)
    List<UUID> findProductIdsBySizeAndColor(@Param("size") String size, @Param("color") String color);

    interface ProductMaxMrpProjection {
        UUID getProductId();
        java.math.BigDecimal getMrp();
    }

    @Query("""
            select v.productId as productId, max(v.mrp) as mrp from ProductVariant v
            where v.productId in :productIds and v.active = true
            group by v.productId
            """)
    List<ProductMaxMrpProjection> findMaxMrpByProductIdIn(@Param("productIds") List<UUID> productIds);
}
