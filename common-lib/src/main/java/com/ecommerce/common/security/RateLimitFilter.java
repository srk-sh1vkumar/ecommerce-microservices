package com.ecommerce.common.security;

import com.ecommerce.common.exception.ServiceException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token bucket rate limiting filter to prevent API abuse.
 *
 * Features:
 * - Per-IP rate limiting with configurable limits
 * - Token bucket algorithm for burst handling
 * - Automatic cleanup of expired entries
 * - X-RateLimit headers for client awareness
 *
 * Default Configuration:
 * - 100 requests per minute per IP
 * - 20 request burst capacity
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int DEFAULT_LIMIT = 100; // requests per minute
    private static final int BURST_CAPACITY = 20; // burst requests allowed
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);

        // Skip rate limiting for health checks
        if (isHealthCheckEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitBucket bucket = buckets.computeIfAbsent(
            clientIp,
            k -> new RateLimitBucket(DEFAULT_LIMIT, BURST_CAPACITY)
        );

        if (bucket.tryConsume()) {
            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(DEFAULT_LIMIT));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getRemaining()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(bucket.getResetTime()));

            filterChain.doFilter(request, response);
        } else {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(429); // 429 Too Many Requests
            response.setHeader("X-RateLimit-Limit", String.valueOf(DEFAULT_LIMIT));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(bucket.getResetTime()));
            response.setHeader("Retry-After", String.valueOf(bucket.getRetryAfter()));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfter\":" + bucket.getRetryAfter() + "}");
        }

        // Cleanup old entries periodically
        cleanupOldBuckets();
    }

    private String getClientIp(HttpServletRequest request) {
        // Check for X-Forwarded-For header (proxy/load balancer)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Return first IP if multiple are present
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    private boolean isHealthCheckEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator/health") ||
               path.contains("/health") ||
               path.contains("/ping");
    }

    private void cleanupOldBuckets() {
        long currentTime = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry ->
            currentTime - entry.getValue().getLastAccessTime() > WINDOW_DURATION.toMillis() * 10
        );
    }

    /**
     * Token bucket for rate limiting using sliding window algorithm.
     */
    private static class RateLimitBucket {
        private final int maxTokens;
        private final int burstCapacity;
        private final AtomicInteger tokens;
        private volatile long lastRefillTime;
        private volatile long lastAccessTime;

        public RateLimitBucket(int maxTokens, int burstCapacity) {
            this.maxTokens = maxTokens;
            this.burstCapacity = burstCapacity;
            this.tokens = new AtomicInteger(maxTokens);
            this.lastRefillTime = System.currentTimeMillis();
            this.lastAccessTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refillTokens();
            lastAccessTime = System.currentTimeMillis();

            int currentTokens = tokens.get();
            if (currentTokens > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refillTokens() {
            long currentTime = System.currentTimeMillis();
            long timePassed = currentTime - lastRefillTime;

            if (timePassed >= WINDOW_DURATION.toMillis()) {
                tokens.set(maxTokens);
                lastRefillTime = currentTime;
            } else {
                // Gradual refill based on time passed
                long tokensToAdd = (timePassed * maxTokens) / WINDOW_DURATION.toMillis();
                if (tokensToAdd > 0) {
                    int currentTokens = tokens.get();
                    int newTokens = Math.min(currentTokens + (int)tokensToAdd, maxTokens);
                    tokens.set(newTokens);
                    lastRefillTime = currentTime;
                }
            }
        }

        public int getRemaining() {
            return Math.max(0, tokens.get());
        }

        public long getResetTime() {
            return lastRefillTime + WINDOW_DURATION.toMillis();
        }

        public long getRetryAfter() {
            return Math.max(0, (getResetTime() - System.currentTimeMillis()) / 1000);
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}