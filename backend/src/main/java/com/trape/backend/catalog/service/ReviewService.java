package com.trape.backend.catalog.service;

import com.trape.backend.auth.repository.UserRepository;
import com.trape.backend.catalog.dto.CatalogDtos.CreateReviewRequest;
import com.trape.backend.catalog.dto.CatalogDtos.ReviewResponse;
import com.trape.backend.catalog.entity.Review;
import com.trape.backend.catalog.repository.ProductRepository;
import com.trape.backend.catalog.repository.ReviewRepository;
import com.trape.backend.common.dto.PageResponse;
import com.trape.backend.common.exception.ApiException;
import com.trape.backend.order.repository.OrderItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                          UserRepository userRepository, OrderItemRepository orderItemRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listByProduct(UUID productId, int page, int pageSize) {
        Page<Review> result = reviewRepository.findByProductIdOrderByCreatedAtDesc(
                productId, PageRequest.of(page, Math.min(pageSize, 100), Sort.by("createdAt").descending()));
        return PageResponse.from(result, result.getContent().stream().map(r -> new ReviewResponse(
                r.getId(), r.getUserId(),
                userRepository.findById(r.getUserId()).map(u -> u.getFullName()).orElse("Trape Customer"),
                r.getRating(), r.getComment(), r.getCreatedAt()
        )).toList());
    }

    @Transactional
    public ReviewResponse addReview(UUID productId, UUID userId, CreateReviewRequest request) {
        if (!productRepository.existsById(productId)) {
            throw ApiException.notFound("Product");
        }
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw ApiException.conflict("ALREADY_REVIEWED", "You have already reviewed this product");
        }
        boolean purchased = orderItemRepository.existsDeliveredPurchaseByUserAndProduct(userId, productId);
        if (!purchased) {
            throw ApiException.badRequest("PURCHASE_REQUIRED", "You can only review products you have purchased");
        }
        Review review = reviewRepository.save(new Review(productId, userId, request.rating(), request.comment()));
        String userName = userRepository.findById(userId).map(u -> u.getFullName()).orElse("Trape Customer");
        return new ReviewResponse(review.getId(), userId, userName, review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
