package com.trape.backend.cart.repository;

import com.trape.backend.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserIdAndStatus(UUID userId, String status);

    Optional<Cart> findByGuestTokenAndStatus(String guestToken, String status);
}
