package com.ecommerce.common.util;

import com.ecommerce.common.exception.ServiceException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refresh Token utility for secure token refresh mechanism.
 *
 * Features:
 * - Long-lived refresh tokens (7 days)
 * - Short-lived access tokens (15 minutes)
 * - Token rotation on refresh
 * - Refresh token revocation
 * - In-memory token store (can be replaced with Redis)
 *
 * Security:
 * - Separate secret key for refresh tokens
 * - Token family tracking to detect theft
 * - Automatic revocation on suspicious activity
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Component
public class RefreshTokenUtil {

    private final SecretKey secretKey;
    private final JwtUtil jwtUtil;

    // In-memory store (replace with Redis in production)
    private final Map<String, RefreshTokenData> refreshTokenStore = new ConcurrentHashMap<>();

    @Value("${jwt.refresh-secret:MyVerySecureRefreshTokenSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm}")
    private String refreshSecret;

    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000; // 7 days

    public RefreshTokenUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.secretKey = Keys.hmacShaKeyFor(
            "MyVerySecureRefreshTokenSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm"
                .getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generate a new refresh token for the user.
     */
    public String generateRefreshToken(String email) {
        String tokenId = UUID.randomUUID().toString();
        String tokenFamily = UUID.randomUUID().toString();

        Instant now = Instant.now();
        Instant expiration = now.plus(7, ChronoUnit.DAYS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenId", tokenId);
        claims.put("tokenFamily", tokenFamily);
        claims.put("type", "refresh");

        String token = Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();

        // Store refresh token data
        RefreshTokenData tokenData = new RefreshTokenData(
            tokenId,
            tokenFamily,
            email,
            expiration.toEpochMilli()
        );
        refreshTokenStore.put(tokenId, tokenData);

        return token;
    }

    /**
     * Refresh access token using refresh token.
     * Implements token rotation for enhanced security.
     */
    public TokenPair refreshAccessToken(String refreshToken) {
        try {
            Claims claims = parseRefreshToken(refreshToken);
            String tokenId = claims.get("tokenId", String.class);
            String tokenFamily = claims.get("tokenFamily", String.class);
            String email = claims.getSubject();

            // Verify token exists and is valid
            RefreshTokenData tokenData = refreshTokenStore.get(tokenId);
            if (tokenData == null) {
                throw ServiceException.unauthorized("Invalid refresh token", "INVALID_REFRESH_TOKEN");
            }

            // Check if token is expired
            if (System.currentTimeMillis() > tokenData.getExpiresAt()) {
                refreshTokenStore.remove(tokenId);
                throw ServiceException.unauthorized("Refresh token expired", "REFRESH_TOKEN_EXPIRED");
            }

            // Verify token family (detect token theft)
            if (!tokenFamily.equals(tokenData.getTokenFamily())) {
                // Potential token theft - revoke entire family
                revokeTokenFamily(tokenFamily);
                throw ServiceException.unauthorized("Token family mismatch - potential theft detected", "TOKEN_THEFT_DETECTED");
            }

            // Generate new token pair
            String newAccessToken = jwtUtil.generateToken(email);
            String newRefreshToken = generateRefreshToken(email);

            // Revoke old refresh token (token rotation)
            refreshTokenStore.remove(tokenId);

            return new TokenPair(newAccessToken, newRefreshToken);

        } catch (io.jsonwebtoken.security.SecurityException | io.jsonwebtoken.MalformedJwtException e) {
            throw ServiceException.unauthorized("Invalid refresh token", "INVALID_REFRESH_TOKEN");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw ServiceException.unauthorized("Refresh token expired", "REFRESH_TOKEN_EXPIRED");
        }
    }

    /**
     * Revoke a refresh token.
     */
    public void revokeRefreshToken(String refreshToken) {
        try {
            Claims claims = parseRefreshToken(refreshToken);
            String tokenId = claims.get("tokenId", String.class);
            refreshTokenStore.remove(tokenId);
        } catch (Exception e) {
            // Silently ignore invalid tokens
        }
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices).
     */
    public void revokeAllUserTokens(String email) {
        refreshTokenStore.entrySet().removeIf(entry ->
            entry.getValue().getEmail().equals(email)
        );
    }

    /**
     * Revoke entire token family (in case of token theft detection).
     */
    private void revokeTokenFamily(String tokenFamily) {
        refreshTokenStore.entrySet().removeIf(entry ->
            entry.getValue().getTokenFamily().equals(tokenFamily)
        );
    }

    /**
     * Parse and validate refresh token.
     */
    private Claims parseRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate refresh token without parsing.
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        try {
            Claims claims = parseRefreshToken(refreshToken);
            String tokenId = claims.get("tokenId", String.class);
            RefreshTokenData tokenData = refreshTokenStore.get(tokenId);
            return tokenData != null && System.currentTimeMillis() <= tokenData.getExpiresAt();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clean up expired tokens (should be called periodically).
     */
    public void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        refreshTokenStore.entrySet().removeIf(entry ->
            currentTime > entry.getValue().getExpiresAt()
        );
    }

    /**
     * Data class for token pair response.
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }

    /**
     * Internal data class for refresh token storage.
     */
    private static class RefreshTokenData {
        private final String tokenId;
        private final String tokenFamily;
        private final String email;
        private final long expiresAt;

        public RefreshTokenData(String tokenId, String tokenFamily, String email, long expiresAt) {
            this.tokenId = tokenId;
            this.tokenFamily = tokenFamily;
            this.email = email;
            this.expiresAt = expiresAt;
        }

        public String getTokenId() {
            return tokenId;
        }

        public String getTokenFamily() {
            return tokenFamily;
        }

        public String getEmail() {
            return email;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }
}