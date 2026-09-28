package com.trape.backend.catalog.repository;

import com.trape.backend.catalog.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    @Query("select avg(r.rating) from Review r where r.productId = :productId")
    Double averageRatingByProductId(@Param("productId") UUID productId);

    long countByProductId(UUID productId);
}
