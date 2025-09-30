package com.ecommerce.common.util;

import com.ecommerce.common.constants.SecurityConstants;
import com.ecommerce.common.exception.ServiceException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for JWT token operations.
 * Handles token generation, validation, and parsing across all microservices.
 *
 * Features:
 * - Token generation with claims
 * - Token validation and parsing
 * - Expiration handling
 * - Secure key management
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey secretKey;
    private final long expirationTime;

    /**
     * Constructor with custom secret and expiration.
     *
     * @param secret JWT secret key
     * @param expirationTime Token expiration time in milliseconds
     */
    public JwtUtil(String secret, long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    /**
     * Constructor with default configuration.
     */
    public JwtUtil() {
        this(SecurityConstants.JWT_SECRET, SecurityConstants.JWT_EXPIRATION);
    }

    /**
     * Generates a JWT token for the given email.
     *
     * @param email User email
     * @return JWT token string
     */
    public String generateToken(String email) {
        return generateToken(email, new HashMap<>());
    }

    /**
     * Generates a JWT token with custom claims.
     *
     * @param email User email
     * @param claims Additional claims to include
     * @return JWT token string
     */
    public String generateToken(String email, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuer(SecurityConstants.JWT_ISSUER)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts email from JWT token.
     *
     * @param token JWT token
     * @return User email
     * @throws ServiceException if token is invalid
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Failed to extract email from token", e);
            throw ServiceException.unauthorized(SecurityConstants.INVALID_TOKEN);
        }
    }

    /**
     * Validates JWT token.
     *
     * @param token JWT token to validate
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            logger.error("Invalid JWT signature or malformed token", e);
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired", e);
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported", e);
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty", e);
        } catch (JwtException e) {
            logger.error("JWT validation error", e);
        }
        return false;
    }

    /**
     * Parses and validates JWT token.
     *
     * @param token JWT token
     * @return Claims from the token
     * @throws JwtException if token is invalid
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Gets expiration date from token.
     *
     * @param token JWT token
     * @return Expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    /**
     * Checks if token is expired.
     *
     * @param token JWT token
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extracts token from Authorization header.
     *
     * @param authHeader Authorization header value
     * @return JWT token without Bearer prefix
     */
    public static String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith(SecurityConstants.JWT_PREFIX)) {
            return authHeader.substring(SecurityConstants.JWT_PREFIX.length());
        }
        return null;
    }
}