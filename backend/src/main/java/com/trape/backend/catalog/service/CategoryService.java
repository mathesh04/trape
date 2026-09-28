package com.trape.backend.catalog.service;

import com.trape.backend.catalog.dto.CatalogDtos.CategoryResponse;
import com.trape.backend.catalog.dto.CatalogDtos.CategoryWriteRequest;
import com.trape.backend.catalog.entity.Category;
import com.trape.backend.catalog.repository.CategoryRepository;
import com.trape.backend.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryWriteRequest request) {
        String slug = (request.slug() == null || request.slug().isBlank())
                ? slugify(request.name())
                : request.slug();
        if (categoryRepository.existsBySlug(slug)) {
            throw ApiException.conflict("SLUG_TAKEN", "A category with slug '" + slug + "' already exists");
        }
        Category category = new Category(request.parentId(), request.name(), slug, request.displayOrder());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryWriteRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category"));
        category.setName(request.name());
        category.setParentId(request.parentId());
        category.setDisplayOrder(request.displayOrder());
        if (request.slug() != null && !request.slug().isBlank()) {
            category.setSlug(request.slug());
        }
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category"));
        category.setActive(false);
        categoryRepository.save(category);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getParentId(), c.getName(), c.getSlug(), c.getDisplayOrder());
    }

    public static String slugify(String input) {
        String normalized = input.toLowerCase().trim().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
        return UriUtils.encode(normalized, StandardCharsets.UTF_8);
    }
}
