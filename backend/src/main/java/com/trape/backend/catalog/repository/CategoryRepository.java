package com.trape.backend.catalog.repository;

import com.trape.backend.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
