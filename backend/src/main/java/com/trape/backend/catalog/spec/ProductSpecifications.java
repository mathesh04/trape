package com.trape.backend.catalog.spec;

import com.trape.backend.catalog.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Whitelisted, composable filters for GET /products — avoids arbitrary
 * column injection since only these specific predicates are exposed.
 *
 * Note: Product has no JPA relation to ProductVariant (see Product entity
 * notes — fetch is explicit, not relational), so size/color filters are
 * resolved by ProductService first querying ProductVariantRepository for
 * matching product ids, then applying idIn() here.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");
    }

    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<Product> nameContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;
            return cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
        };
    }

    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("basePrice"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("basePrice"), min);
            return cb.lessThanOrEqualTo(root.get("basePrice"), max);
        };
    }

    public static Specification<Product> idIn(List<UUID> ids) {
        return (root, query, cb) -> ids == null ? null : root.get("id").in(ids);
    }
}
