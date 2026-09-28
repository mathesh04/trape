package com.trape.backend.cart.service;

import com.trape.backend.cart.dto.CartDtos.*;
import com.trape.backend.cart.entity.Cart;
import com.trape.backend.cart.entity.CartItem;
import com.trape.backend.cart.repository.CartItemRepository;
import com.trape.backend.cart.repository.CartRepository;
import com.trape.backend.catalog.entity.Product;
import com.trape.backend.catalog.entity.ProductImage;
import com.trape.backend.catalog.entity.ProductVariant;
import com.trape.backend.catalog.repository.ProductImageRepository;
import com.trape.backend.catalog.repository.ProductRepository;
import com.trape.backend.catalog.repository.ProductVariantRepository;
import com.trape.backend.common.exception.ApiException;
import com.trape.backend.inventory.exception.OutOfStockException;
import com.trape.backend.inventory.service.InventoryService;
import com.trape.backend.promotion.entity.Coupon;
import com.trape.backend.promotion.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A cart belongs to exactly one of: an authenticated user (user_id) or a
 * guest session (guest_token, sent by the frontend as X-Guest-Token and
 * persisted client-side). On login, the guest cart's items are merged into
 * the user's cart (see mergeGuestCartIntoUser) and the guest cart is retired.
 */
@Service
public class CartService {

    private static final String ACTIVE = "ACTIVE";

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                        ProductVariantRepository variantRepository, ProductRepository productRepository,
                        ProductImageRepository productImageRepository, InventoryService inventoryService,
                        CouponService couponService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.inventoryService = inventoryService;
        this.couponService = couponService;
    }

    @Transactional
    public Cart getOrCreateCart(UUID userId, String guestToken) {
        if (userId != null) {
            return cartRepository.findByUserIdAndStatus(userId, ACTIVE)
                    .orElseGet(() -> cartRepository.save(new Cart(userId, null)));
        }
        String token = (guestToken == null || guestToken.isBlank()) ? UUID.randomUUID().toString() : guestToken;
        return cartRepository.findByGuestTokenAndStatus(token, ACTIVE)
                .orElseGet(() -> cartRepository.save(new Cart(null, token)));
    }

    @Transactional
    public CartResponse addItem(UUID userId, String guestToken, AddItemRequest request) {
        Cart cart = getOrCreateCart(userId, guestToken);
        ProductVariant variant = variantRepository.findById(request.variantId())
                .filter(ProductVariant::isActive)
                .orElseThrow(() -> ApiException.notFound("Product variant"));

        Map<UUID, Integer> stock = inventoryService.sellableQuantitiesByVariant(List.of(variant.getId()));
        int available = stock.getOrDefault(variant.getId(), 0);

        CartItem existing = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId()).orElse(null);
        int desiredQty = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        if (desiredQty > available) {
            throw new OutOfStockException(variant.getSku());
        }

        if (existing != null) {
            existing.setQuantity(desiredQty);
            existing.setPriceSnapshot(variant.getPrice());
            cartItemRepository.save(existing);
        } else {
            cartItemRepository.save(new CartItem(cart.getId(), variant.getId(), request.quantity(), variant.getPrice()));
        }
        cart.touch();
        cartRepository.save(cart);
        return assemble(cart);
    }

    @Transactional
    public CartResponse updateItem(UUID userId, String guestToken, UUID itemId, UpdateItemRequest request) {
        Cart cart = getOrCreateCart(userId, guestToken);
        CartItem item = cartItemRepository.findById(itemId)
                .filter(i -> i.getCartId().equals(cart.getId()))
                .orElseThrow(() -> ApiException.notFound("Cart item"));

        Map<UUID, Integer> stock = inventoryService.sellableQuantitiesByVariant(List.of(item.getVariantId()));
        if (request.quantity() > stock.getOrDefault(item.getVariantId(), 0)) {
            throw ApiException.unprocessable("OUT_OF_STOCK", "Requested quantity exceeds available stock");
        }
        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        cart.touch();
        cartRepository.save(cart);
        return assemble(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, String guestToken, UUID itemId) {
        Cart cart = getOrCreateCart(userId, guestToken);
        CartItem item = cartItemRepository.findById(itemId)
                .filter(i -> i.getCartId().equals(cart.getId()))
                .orElseThrow(() -> ApiException.notFound("Cart item"));
        cartItemRepository.delete(item);
        cart.touch();
        cartRepository.save(cart);
        return assemble(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse view(UUID userId, String guestToken) {
        Cart cart = getOrCreateCart(userId, guestToken);
        return assemble(cart);
    }

    @Transactional
    public CartResponse applyCoupon(UUID userId, String guestToken, String code) {
        Cart cart = getOrCreateCart(userId, guestToken);
        BigDecimal subtotal = computeSubtotal(cart.getId());
        Coupon coupon = couponService.validateForCart(code, subtotal);
        cart.setCouponId(coupon.getId());
        cartRepository.save(cart);
        return assemble(cart);
    }

    @Transactional
    public CartResponse removeCoupon(UUID userId, String guestToken) {
        Cart cart = getOrCreateCart(userId, guestToken);
        cart.setCouponId(null);
        cartRepository.save(cart);
        return assemble(cart);
    }

    /** Called by AuthService right after login — folds guest cart items into the user's cart. */
    @Transactional
    public void mergeGuestCartIntoUser(UUID userId, String guestToken) {
        if (guestToken == null || guestToken.isBlank()) return;
        cartRepository.findByGuestTokenAndStatus(guestToken, ACTIVE).ifPresent(guestCart -> {
            Cart userCart = cartRepository.findByUserIdAndStatus(userId, ACTIVE)
                    .orElseGet(() -> cartRepository.save(new Cart(userId, null)));
            for (CartItem guestItem : cartItemRepository.findByCartId(guestCart.getId())) {
                CartItem existing = cartItemRepository.findByCartIdAndVariantId(userCart.getId(), guestItem.getVariantId()).orElse(null);
                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + guestItem.getQuantity());
                    cartItemRepository.save(existing);
                } else {
                    cartItemRepository.save(new CartItem(userCart.getId(), guestItem.getVariantId(),
                            guestItem.getQuantity(), guestItem.getPriceSnapshot()));
                }
            }
            guestCart.setStatus("CONVERTED");
            cartRepository.save(guestCart);
        });
    }

    private BigDecimal computeSubtotal(UUID cartId) {
        return cartItemRepository.findByCartId(cartId).stream()
                .map(i -> i.getPriceSnapshot().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartResponse assemble(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<UUID> variantIds = items.stream().map(CartItem::getVariantId).toList();

        Map<UUID, ProductVariant> variantsById = variantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        List<UUID> productIds = variantsById.values().stream().map(ProductVariant::getProductId).distinct().toList();
        Map<UUID, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<UUID, String> primaryImageByProduct = productImageRepository.findByProductIdInOrderByDisplayOrderAsc(productIds).stream()
                .collect(Collectors.toMap(ProductImage::getProductId, ProductImage::getUrl, (a, b) -> a));
        Map<UUID, Integer> stockByVariant = inventoryService.sellableQuantitiesByVariant(variantIds);

        List<CartItemResponse> itemResponses = items.stream().map(item -> {
            ProductVariant variant = variantsById.get(item.getVariantId());
            Product product = variant == null ? null : productsById.get(variant.getProductId());
            int available = stockByVariant.getOrDefault(item.getVariantId(), 0);
            return new CartItemResponse(
                    item.getId(), item.getVariantId(),
                    product == null ? "Unknown product" : product.getName(),
                    product == null ? null : product.getSlug(),
                    product == null ? null : primaryImageByProduct.get(product.getId()),
                    variant == null ? null : variant.getSize(),
                    variant == null ? null : variant.getColor(),
                    item.getQuantity(), item.getPriceSnapshot(),
                    item.getPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity())),
                    available >= item.getQuantity(), available
            );
        }).toList();

        BigDecimal subtotal = itemResponses.stream().map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = null;
        if (cart.getCouponId() != null) {
            try {
                // re-validate on every read so an expired/exhausted coupon silently stops applying
                var coupon = couponService.validateForCartById(cart.getCouponId(), subtotal);
                discount = coupon.computeDiscount(subtotal);
                couponCode = coupon.getCode();
            } catch (ApiException ignored) {
                // coupon no longer valid — the discount simply won't be applied on this read;
                // the frontend shows the cart without it and the user can remove/replace it
            }
        }

        return new CartResponse(cart.getId(), itemResponses, subtotal, discount, couponCode,
                subtotal.subtract(discount), cart.getGuestToken());
    }
}
