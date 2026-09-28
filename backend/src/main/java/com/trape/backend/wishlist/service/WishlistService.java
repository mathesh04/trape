package com.trape.backend.wishlist.service;

import com.trape.backend.catalog.dto.CatalogDtos.ProductSummaryResponse;
import com.trape.backend.catalog.entity.Product;
import com.trape.backend.catalog.entity.ProductImage;
import com.trape.backend.catalog.repository.ProductImageRepository;
import com.trape.backend.catalog.repository.ProductRepository;
import com.trape.backend.catalog.repository.ProductVariantRepository;
import com.trape.backend.catalog.repository.ReviewRepository;
import com.trape.backend.common.exception.ApiException;
import com.trape.backend.wishlist.entity.WishlistItem;
import com.trape.backend.wishlist.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ReviewRepository reviewRepository;

    public WishlistService(WishlistItemRepository wishlistItemRepository, ProductRepository productRepository,
                            ProductImageRepository productImageRepository, ProductVariantRepository productVariantRepository,
                            ReviewRepository reviewRepository) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productVariantRepository = productVariantRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> list(UUID userId) {
        List<UUID> productIds = wishlistItemRepository.findByUserId(userId).stream()
                .map(WishlistItem::getProductId).toList();
        List<Product> products = productRepository.findAllById(productIds);
        Map<UUID, String> images = productImageRepository.findByProductIdInOrderByDisplayOrderAsc(productIds).stream()
                .collect(Collectors.toMap(ProductImage::getProductId, ProductImage::getUrl, (a, b) -> a));
        Map<UUID, java.math.BigDecimal> maxMrpByProduct = productIds.isEmpty() ? Map.of() :
                productVariantRepository.findMaxMrpByProductIdIn(productIds).stream()
                        .collect(Collectors.toMap(
                                ProductVariantRepository.ProductMaxMrpProjection::getProductId,
                                ProductVariantRepository.ProductMaxMrpProjection::getMrp,
                                (a, b) -> a));
        return products.stream().map(p -> new ProductSummaryResponse(
                p.getId(), p.getName(), p.getSlug(), p.getBrand(), p.getBasePrice(), images.get(p.getId()),
                reviewRepository.averageRatingByProductId(p.getId()), reviewRepository.countByProductId(p.getId()),
                maxMrpByProduct.get(p.getId())
        )).toList();
    }

    @Transactional
    public void add(UUID userId, UUID productId) {
        if (!productRepository.existsById(productId)) throw ApiException.notFound("Product");
        if (wishlistItemRepository.findByUserIdAndProductId(userId, productId).isEmpty()) {
            wishlistItemRepository.save(new WishlistItem(userId, productId));
        }
    }

    @Transactional
    public void remove(UUID userId, UUID productId) {
        wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
    }
}
