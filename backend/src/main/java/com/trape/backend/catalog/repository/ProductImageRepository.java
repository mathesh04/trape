package com.trape.backend.catalog.repository;

import com.trape.backend.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(UUID productId);

    List<ProductImage> findByProductIdInOrderByDisplayOrderAsc(List<UUID> productIds);

    void deleteByProductId(UUID productId);
}
