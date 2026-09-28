package com.trape.backend.catalog.service;

import com.trape.backend.catalog.dto.CatalogDtos.*;
import com.trape.backend.catalog.entity.Product;
import com.trape.backend.catalog.entity.ProductImage;
import com.trape.backend.catalog.entity.ProductVariant;
import com.trape.backend.catalog.repository.ProductImageRepository;
import com.trape.backend.catalog.repository.ProductRepository;
import com.trape.backend.catalog.repository.ProductVariantRepository;
import com.trape.backend.catalog.repository.ReviewRepository;
import com.trape.backend.catalog.spec.ProductSpecifications;
import com.trape.backend.common.dto.PageResponse;
import com.trape.backend.common.exception.ApiException;
import com.trape.backend.inventory.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ReviewRepository reviewRepository;
    private final InventoryService inventoryService;

    public ProductService(ProductRepository productRepository,
                           ProductImageRepository productImageRepository,
                           ProductVariantRepository productVariantRepository,
                           ReviewRepository reviewRepository,
                           InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productVariantRepository = productVariantRepository;
        this.reviewRepository = reviewRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> search(
            UUID categoryId, String q, String size, String color,
            BigDecimal minPrice, BigDecimal maxPrice,
            String sort, int page, int pageSize) {

        Specification<Product> spec = Specification.where(ProductSpecifications.isActive())
                .and(ProductSpecifications.hasCategory(categoryId))
                .and(ProductSpecifications.nameContains(q))
                .and(ProductSpecifications.priceBetween(minPrice, maxPrice));

        if (size != null || color != null) {
            List<UUID> matchingIds = productVariantRepository.findProductIdsBySizeAndColor(size, color);
            spec = spec.and(ProductSpecifications.idIn(matchingIds));
        }

        Sort sortOrder = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by("basePrice").ascending();
            case "price_desc" -> Sort.by("basePrice").descending();
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.by("createdAt").descending();
        };

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(page, Math.min(pageSize, 100), sortOrder));

        List<UUID> productIds = result.getContent().stream().map(Product::getId).toList();
        Map<UUID, String> primaryImages = productImageRepository.findByProductIdInOrderByDisplayOrderAsc(productIds).stream()
                .collect(Collectors.toMap(ProductImage::getProductId, ProductImage::getUrl, (a, b) -> a));
        Map<UUID, java.math.BigDecimal> maxMrpByProduct = productIds.isEmpty() ? Map.of() :
                productVariantRepository.findMaxMrpByProductIdIn(productIds).stream()
                        .collect(Collectors.toMap(
                                ProductVariantRepository.ProductMaxMrpProjection::getProductId,
                                ProductVariantRepository.ProductMaxMrpProjection::getMrp,
                                (a, b) -> a));

        List<ProductSummaryResponse> content = result.getContent().stream().map(p -> new ProductSummaryResponse(
                p.getId(), p.getName(), p.getSlug(), p.getBrand(), p.getBasePrice(),
                primaryImages.get(p.getId()),
                reviewRepository.averageRatingByProductId(p.getId()),
                reviewRepository.countByProductId(p.getId()),
                maxMrpByProduct.get(p.getId())
        )).toList();

        return PageResponse.from(result, content);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Product"));
        return assembleDetail(product);
    }

    @Transactional
    public ProductDetailResponse create(ProductWriteRequest request) {
        String slug = (request.slug() == null || request.slug().isBlank())
                ? CategoryService.slugify(request.name())
                : request.slug();
        if (productRepository.existsBySlug(slug)) {
            throw ApiException.conflict("SLUG_TAKEN", "A product with slug '" + slug + "' already exists");
        }
        Product product = new Product(request.categoryId(), request.name(), slug,
                request.description(), request.brand() == null ? "Trape" : request.brand(),
                request.basePrice());
        product.setStatus(request.status() == null ? "DRAFT" : request.status());
        product = productRepository.save(product);

        saveImages(product.getId(), request.imageUrls());
        saveVariants(product.getId(), request.variants());

        return assembleDetail(product);
    }

    @Transactional
    public ProductDetailResponse update(UUID id, ProductWriteRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product"));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBrand(request.brand() == null ? product.getBrand() : request.brand());
        product.setBasePrice(request.basePrice());
        product.setCategoryId(request.categoryId());
        if (request.status() != null) product.setStatus(request.status());
        if (request.slug() != null && !request.slug().isBlank()) product.setSlug(request.slug());
        product = productRepository.save(product);

        if (request.imageUrls() != null) {
            productImageRepository.deleteByProductId(product.getId());
            saveImages(product.getId(), request.imageUrls());
        }
        if (request.variants() != null) {
            saveVariants(product.getId(), request.variants());
        }

        return assembleDetail(product);
    }

    @Transactional
    public void archive(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product"));
        product.setStatus("ARCHIVED");
        productRepository.save(product);
    }

    private void saveImages(UUID productId, List<String> urls) {
        if (urls == null) return;
        int order = 0;
        for (String url : urls) {
            productImageRepository.save(new ProductImage(productId, url, null, order++));
        }
    }

    private void saveVariants(UUID productId, List<VariantWriteRequest> variants) {
        if (variants == null) return;
        for (VariantWriteRequest v : variants) {
            ProductVariant variant;
            if (v.id() != null) {
                variant = productVariantRepository.findById(v.id())
                        .orElseThrow(() -> ApiException.notFound("Variant"));
                variant.setSize(v.size());
                variant.setColor(v.color());
                variant.setPrice(v.price());
                variant.setMrp(v.mrp());
            } else {
                if (productVariantRepository.existsBySku(v.sku())) {
                    throw ApiException.conflict("SKU_TAKEN", "SKU '" + v.sku() + "' already exists");
                }
                variant = new ProductVariant(productId, v.sku(), v.size(), v.color(), v.price(), v.mrp());
            }
            variant = productVariantRepository.save(variant);
            if (v.initialStock() != null) {
                inventoryService.setStock(variant.getId(), v.initialStock());
            }
        }
    }

    private ProductDetailResponse assembleDetail(Product product) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderBySizeAsc(product.getId());

        Map<UUID, Integer> stockByVariant = inventoryService.sellableQuantitiesByVariant(
                variants.stream().map(ProductVariant::getId).toList());

        List<ProductImageResponse> imageResponses = images.stream()
                .map(i -> new ProductImageResponse(i.getId(), i.getUrl(), i.getAltText(), i.getDisplayOrder()))
                .toList();

        List<VariantResponse> variantResponses = variants.stream().map(v -> {
            int stock = stockByVariant.getOrDefault(v.getId(), 0);
            return new VariantResponse(v.getId(), v.getSku(), v.getSize(), v.getColor(), v.getPrice(), v.getMrp(), stock > 0, stock);
        }).toList();

        return new ProductDetailResponse(
                product.getId(), product.getName(), product.getSlug(), product.getDescription(), product.getBrand(),
                product.getBasePrice(), product.getStatus(), product.getCategoryId(),
                imageResponses, variantResponses,
                reviewRepository.averageRatingByProductId(product.getId()),
                reviewRepository.countByProductId(product.getId())
        );
    }
}
