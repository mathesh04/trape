package com.trape.backend.auth.service;

import com.trape.backend.auth.dto.AuthDtos.*;
import com.trape.backend.auth.entity.RefreshToken;
import com.trape.backend.auth.entity.User;
import com.trape.backend.auth.repository.RefreshTokenRepository;
import com.trape.backend.auth.repository.UserRepository;
import com.trape.backend.common.exception.ApiException;
import com.trape.backend.common.util.HashUtil;
import com.trape.backend.cart.service.CartService;
import com.trape.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CartService cartService;

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        CartService cartService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cartService = cartService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        User user = new User(
                request.email().toLowerCase(),
                request.phone(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                "CUSTOMER"
        );
        user = userRepository.save(user);
        return issueTokenPair(user, null);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        return login(request, null);
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String guestToken) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));

        if (!user.isActive()) {
            throw ApiException.forbidden( "This account has been disabled");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        if (guestToken != null && !guestToken.isBlank()) {
            cartService.mergeGuestCartIntoUser(user.getId(), guestToken);
        }
        return issueTokenPair(user, null);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String hash = HashUtil.sha256(request.refreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));

        if (!existing.isValid()) {
            throw ApiException.unauthorized("Refresh token expired or revoked");
        }
        existing.revoke(); // rotate — one-time use
        refreshTokenRepository.save(existing);

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> ApiException.notFound("User"));
        return issueTokenPair(user, null);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(HashUtil.sha256(rawRefreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    private TokenResponse issueTokenPair(User user, String deviceInfo) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = jwtService.generateOpaqueRefreshToken();

        RefreshToken tokenEntity = new RefreshToken(
                user.getId(),
                HashUtil.sha256(rawRefreshToken),
                deviceInfo,
                Instant.now().plus(jwtService.getRefreshTokenTtlDays(), ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(tokenEntity);

        UserSummary summary = new UserSummary(user.getId(), user.getEmail(), user.getPhone(), user.getFullName(), user.getRole());
        return new TokenResponse(
                accessToken,
                rawRefreshToken,
                jwtService.getAccessTokenTtlMinutes() * 60,
                summary
        );
    }
}
