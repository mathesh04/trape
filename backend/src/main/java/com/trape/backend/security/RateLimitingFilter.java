package com.trape.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trape.backend.common.dto.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiting filter using Bucket4j token bucket algorithm.
 * Protects auth endpoints against brute-force attacks and general API against abuse.
 */
@Component
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory bucket registries keyed by client IP
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> paymentBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Skip rate limiting for static assets, swagger, and actuator health checks
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        Bucket bucket = resolveBucket(path, method, clientIp);

        if (bucket != null && !bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");

            String traceId = UUID.randomUUID().toString();
            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Please wait a moment before trying again.",
                    traceId
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Bucket resolveBucket(String path, String method, String clientIp) {
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/auth/login")) {
            // 5 requests per minute for login attempts
            return loginBuckets.computeIfAbsent(clientIp, k ->
                    Bucket.builder()
                            .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                            .build()
            );
        } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/auth/register")) {
            // 5 registration attempts per minute
            return registerBuckets.computeIfAbsent(clientIp, k ->
                    Bucket.builder()
                            .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                            .build()
            );
        } else if ("POST".equalsIgnoreCase(method) && path.contains("/payments/verify")) {
            // 10 payment verification attempts per minute
            return paymentBuckets.computeIfAbsent(clientIp, k ->
                    Bucket.builder()
                            .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                            .build()
            );
        } else if (path.startsWith("/api/v1/")) {
            // General API limit: 120 requests per minute
            return generalBuckets.computeIfAbsent(clientIp, k ->
                    Bucket.builder()
                            .addLimit(Bandwidth.classic(120, Refill.greedy(120, Duration.ofMinutes(1))))
                            .build()
            );
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
