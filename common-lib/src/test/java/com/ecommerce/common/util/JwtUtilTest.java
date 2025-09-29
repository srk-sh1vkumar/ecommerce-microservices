package com.ecommerce.common.util;

import com.ecommerce.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for JwtUtil.
 * Tests token generation, validation, and parsing.
 */
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, 3600000); // 1 hour expiration
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid token for email")
        void testGenerateToken() {
            String token = jwtUtil.generateToken(TEST_EMAIL);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("Should generate token with custom claims")
        void testGenerateTokenWithClaims() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "ADMIN");
            claims.put("department", "IT");

            String token = jwtUtil.generateToken(TEST_EMAIL, claims);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate different tokens for same email")
        void testTokenUniqueness() {
            String token1 = jwtUtil.generateToken(TEST_EMAIL);
            // Small delay to ensure different timestamps
            try { Thread.sleep(10); } catch (InterruptedException e) {}
            String token2 = jwtUtil.generateToken(TEST_EMAIL);

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate correct token")
        void testValidToken() {
            String token = jwtUtil.generateToken(TEST_EMAIL);
            boolean isValid = jwtUtil.validateToken(token);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void testMalformedToken() {
            boolean isValid = jwtUtil.validateToken("invalid.token.format");

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject empty token")
        void testEmptyToken() {
            boolean isValid = jwtUtil.validateToken("");

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject null token")
        void testNullToken() {
            boolean isValid = jwtUtil.validateToken(null);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject token with wrong signature")
        void testWrongSignature() {
            JwtUtil anotherUtil = new JwtUtil("different-secret-key-for-testing-purposes-only-long-enough", 3600000);
            String token = anotherUtil.generateToken(TEST_EMAIL);

            // JJWT 0.12.3 throws exception for invalid signature,
            // validateToken catches it and returns false
            boolean isValid = jwtUtil.validateToken(token);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void testExpiredToken() {
            // Create token with 1 millisecond expiration
            JwtUtil shortLivedUtil = new JwtUtil(TEST_SECRET, 1);
            String token = shortLivedUtil.generateToken(TEST_EMAIL);

            // Wait for token to expire
            try { Thread.sleep(10); } catch (InterruptedException e) {}

            boolean isValid = shortLivedUtil.validateToken(token);
            assertThat(isValid).isFalse();

            boolean isExpired = shortLivedUtil.isTokenExpired(token);
            assertThat(isExpired).isTrue();
        }
    }

    @Nested
    @DisplayName("Token Parsing Tests")
    class TokenParsingTests {

        @Test
        @DisplayName("Should extract email from valid token")
        void testExtractEmail() {
            String token = jwtUtil.generateToken(TEST_EMAIL);
            String extractedEmail = jwtUtil.getEmailFromToken(token);

            assertThat(extractedEmail).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should throw exception for invalid token when extracting email")
        void testExtractEmailFromInvalidToken() {
            assertThatThrownBy(() -> jwtUtil.getEmailFromToken("invalid.token"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Invalid");
        }

        @Test
        @DisplayName("Should get expiration date from token")
        void testGetExpirationDate() {
            String token = jwtUtil.generateToken(TEST_EMAIL);

            assertThatNoException().isThrownBy(() -> {
                var expirationDate = jwtUtil.getExpirationDateFromToken(token);
                assertThat(expirationDate).isNotNull();
                assertThat(expirationDate.getTime()).isGreaterThan(System.currentTimeMillis());
            });
        }

        @Test
        @DisplayName("Should check if token is expired")
        void testIsTokenExpired() {
            String token = jwtUtil.generateToken(TEST_EMAIL);
            boolean isExpired = jwtUtil.isTokenExpired(token);

            assertThat(isExpired).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Header Extraction Tests")
    class TokenHeaderExtractionTests {

        @Test
        @DisplayName("Should extract token from Bearer header")
        void testExtractTokenFromHeader() {
            String token = "sample.jwt.token";
            String authHeader = "Bearer " + token;

            String extracted = JwtUtil.extractTokenFromHeader(authHeader);

            assertThat(extracted).isEqualTo(token);
        }

        @Test
        @DisplayName("Should return null for header without Bearer prefix")
        void testExtractTokenWithoutBearer() {
            String extracted = JwtUtil.extractTokenFromHeader("sample.jwt.token");

            assertThat(extracted).isNull();
        }

        @Test
        @DisplayName("Should return null for null header")
        void testExtractTokenFromNullHeader() {
            String extracted = JwtUtil.extractTokenFromHeader(null);

            assertThat(extracted).isNull();
        }

        @Test
        @DisplayName("Should return null for empty header")
        void testExtractTokenFromEmptyHeader() {
            String extracted = JwtUtil.extractTokenFromHeader("");

            assertThat(extracted).isNull();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should complete full token lifecycle")
        void testFullTokenLifecycle() {
            // Generate token
            String token = jwtUtil.generateToken(TEST_EMAIL);
            assertThat(token).isNotNull();

            // Validate token
            assertThat(jwtUtil.validateToken(token)).isTrue();

            // Extract email
            String extractedEmail = jwtUtil.getEmailFromToken(token);
            assertThat(extractedEmail).isEqualTo(TEST_EMAIL);

            // Check expiration
            assertThat(jwtUtil.isTokenExpired(token)).isFalse();

            // Extract from header
            String authHeader = "Bearer " + token;
            String extractedToken = JwtUtil.extractTokenFromHeader(authHeader);
            assertThat(extractedToken).isEqualTo(token);
        }

        @Test
        @DisplayName("Should handle multiple users concurrently")
        void testMultipleUsers() {
            String user1Token = jwtUtil.generateToken("user1@example.com");
            String user2Token = jwtUtil.generateToken("user2@example.com");
            String user3Token = jwtUtil.generateToken("user3@example.com");

            assertThat(user1Token).isNotEqualTo(user2Token);
            assertThat(user2Token).isNotEqualTo(user3Token);

            assertThat(jwtUtil.getEmailFromToken(user1Token)).isEqualTo("user1@example.com");
            assertThat(jwtUtil.getEmailFromToken(user2Token)).isEqualTo("user2@example.com");
            assertThat(jwtUtil.getEmailFromToken(user3Token)).isEqualTo("user3@example.com");
        }
    }
}