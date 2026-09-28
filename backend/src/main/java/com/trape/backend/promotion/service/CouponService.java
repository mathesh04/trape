package com.trape.backend.promotion.service;

import com.trape.backend.common.exception.ApiException;
import com.trape.backend.promotion.dto.CouponDtos.CouponResponse;
import com.trape.backend.promotion.dto.CouponDtos.CouponWriteRequest;
import com.trape.backend.promotion.entity.Coupon;
import com.trape.backend.promotion.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional(readOnly = true)
    public Coupon validateForCart(String code, BigDecimal cartSubtotal) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> ApiException.badRequest("INVALID_COUPON", "Coupon code not found"));
        if (!coupon.isCurrentlyValid()) {
            throw ApiException.badRequest("COUPON_EXPIRED", "This coupon is no longer valid");
        }
        if (coupon.computeDiscount(cartSubtotal).compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("MIN_CART_VALUE_NOT_MET",
                    "Cart must be at least " + coupon.getMinCartValue() + " to use this coupon");
        }
        return coupon;
    }

    /** Same validation as {@link #validateForCart}, but looked up by id (cart stores couponId, not code). */
    @Transactional(readOnly = true)
    public Coupon validateForCartById(UUID couponId, BigDecimal cartSubtotal) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_COUPON", "Coupon no longer exists"));
        if (!coupon.isCurrentlyValid()) {
            throw ApiException.badRequest("COUPON_EXPIRED", "This coupon is no longer valid");
        }
        if (coupon.computeDiscount(cartSubtotal).compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("MIN_CART_VALUE_NOT_MET",
                    "Cart must be at least " + coupon.getMinCartValue() + " to use this coupon");
        }
        return coupon;
    }

    @Transactional
    public void markUsed(UUID couponId) {
        couponRepository.findById(couponId).ifPresent(c -> {
            c.incrementUsage();
            couponRepository.save(c);
        });
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> listAll() {
        return couponRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CouponResponse create(CouponWriteRequest request) {
        if (couponRepository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("CODE_TAKEN", "Coupon code already exists");
        }
        Coupon coupon = new Coupon(request.code().toUpperCase(), request.discountType(), request.discountValue(),
                request.minCartValue() == null ? BigDecimal.ZERO : request.minCartValue(),
                request.validFrom(), request.validUntil(), request.usageLimit());
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public void deactivate(UUID id) {
        Coupon coupon = couponRepository.findById(id).orElseThrow(() -> ApiException.notFound("Coupon"));
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    private CouponResponse toResponse(Coupon c) {
        return new CouponResponse(c.getId(), c.getCode(), c.getDiscountType(), c.getDiscountValue(),
                c.getMinCartValue(), c.getValidFrom(), c.getValidUntil(), c.getUsageLimit(), c.getUsageCount(), c.isActive());
    }
}
