package com.ecommerce.common.util;

import com.ecommerce.common.constants.ErrorCodes;
import com.ecommerce.common.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for ValidationUtils.
 * Tests all validation methods with edge cases and error scenarios.
 */
@DisplayName("ValidationUtils Tests")
class ValidationUtilsTest {

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("Should accept valid email addresses")
        void testValidEmail() {
            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validateEmail("test@example.com"));

            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validateEmail("user.name+tag@domain.co.uk"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "invalid-email",
            "@example.com",
            "user@",
            "user @example.com",
            "",
            "user@@example.com"
        })
        @DisplayName("Should reject invalid email addresses")
        void testInvalidEmail(String email) {
            assertThatThrownBy(() -> ValidationUtils.validateEmail(email))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("email"));
        }

        @Test
        @DisplayName("Should normalize email to lowercase and trim spaces")
        void testEmailNormalization() {
            String normalized = ValidationUtils.normalizeEmail("  Test@Example.COM  ");
            assertThat(normalized).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should handle null email in normalization")
        void testNullEmailNormalization() {
            String normalized = ValidationUtils.normalizeEmail(null);
            assertThat(normalized).isNull();
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should accept strong password")
        void testValidPassword() {
            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validatePassword("StrongP@ss123"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "short",           // Too short
            "alllowercase1",   // No uppercase
            "ALLUPPERCASE1",   // No lowercase
            "NoDigitsHere",    // No digits
            "YOUR_SECURE_PASSWORD",     // Contains weak pattern
            "YOUR_ADMIN_PASSWORD4",       // Contains weak pattern
            "qwerty123"        // Contains weak pattern
        })
        @DisplayName("Should reject weak passwords")
        void testWeakPasswords(String password) {
            assertThatThrownBy(() -> ValidationUtils.validatePassword(password))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCodes.WEAK_PASSWORD);
                });
        }

        @Test
        @DisplayName("Should reject null or blank password")
        void testNullPassword() {
            assertThatThrownBy(() -> ValidationUtils.validatePassword(null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("required");
        }
    }

    @Nested
    @DisplayName("Field Validation Tests")
    class FieldValidationTests {

        @Test
        @DisplayName("Should validate non-blank fields")
        void testValidNotBlank() {
            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validateNotBlank("Some Value", "Field"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should reject blank fields")
        void testInvalidNotBlank(String value) {
            assertThatThrownBy(() -> ValidationUtils.validateNotBlank(value, "TestField"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("TestField")
                .hasMessageContaining("required");
        }

        @Test
        @DisplayName("Should validate IDs")
        void testValidId() {
            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validateId("12345", "Entity"));
        }

        @Test
        @DisplayName("Should reject null or empty IDs")
        void testInvalidId() {
            assertThatThrownBy(() -> ValidationUtils.validateId("", "Product"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Product ID")
                .hasMessageContaining("required");
        }
    }

    @Nested
    @DisplayName("Number Validation Tests")
    class NumberValidationTests {

        @Test
        @DisplayName("Should accept positive numbers")
        void testValidPositive() {
            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validatePositive(10, "Quantity"));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("Should reject non-positive numbers")
        void testInvalidPositive(int value) {
            assertThatThrownBy(() -> ValidationUtils.validatePositive(value, "Stock"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("Should reject null numbers")
        void testNullPositive() {
            assertThatThrownBy(() -> ValidationUtils.validatePositive(null, "Amount"))
                .isInstanceOf(ServiceException.class);
        }
    }

    @Nested
    @DisplayName("Price Validation Tests")
    class PriceValidationTests {

        @Test
        @DisplayName("Should accept valid prices")
        void testValidPrice() {
            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validatePrice(new BigDecimal("99.99")));

            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validatePrice(new BigDecimal("0.01")));
        }

        @Test
        @DisplayName("Should reject zero or negative prices")
        void testInvalidPrice() {
            assertThatThrownBy(() -> ValidationUtils.validatePrice(BigDecimal.ZERO))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCodes.INVALID_PRICE);
                });

            assertThatThrownBy(() -> ValidationUtils.validatePrice(new BigDecimal("-10")))
                .isInstanceOf(ServiceException.class);
        }

        @Test
        @DisplayName("Should reject prices with more than 2 decimal places")
        void testInvalidDecimalPlaces() {
            assertThatThrownBy(() -> ValidationUtils.validatePrice(new BigDecimal("99.999")))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("decimal places");
        }

        @Test
        @DisplayName("Should reject null price")
        void testNullPrice() {
            assertThatThrownBy(() -> ValidationUtils.validatePrice(null))
                .isInstanceOf(ServiceException.class);
        }
    }

    @Nested
    @DisplayName("Quantity Validation Tests")
    class QuantityValidationTests {

        @Test
        @DisplayName("Should accept valid quantities (1-100)")
        void testValidQuantity() {
            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validateQuantity(1));

            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validateQuantity(50));

            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validateQuantity(100));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 101, 1000})
        @DisplayName("Should reject invalid quantities")
        void testInvalidQuantity(int quantity) {
            assertThatThrownBy(() -> ValidationUtils.validateQuantity(quantity))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCodes.INVALID_QUANTITY);
                });
        }

        @Test
        @DisplayName("Should reject null quantity")
        void testNullQuantity() {
            assertThatThrownBy(() -> ValidationUtils.validateQuantity(null))
                .isInstanceOf(ServiceException.class);
        }
    }

    @Nested
    @DisplayName("Stock Validation Tests")
    class StockValidationTests {

        @Test
        @DisplayName("Should accept when stock is sufficient")
        void testSufficientStock() {
            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validateStock(5, 10));

            assertThatNoException().isThrownBy(() ->
                ValidationUtils.validateStock(10, 10));
        }

        @Test
        @DisplayName("Should reject when stock is insufficient")
        void testInsufficientStock() {
            assertThatThrownBy(() -> ValidationUtils.validateStock(10, 5))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Insufficient stock")
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCodes.INSUFFICIENT_STOCK);
                });
        }

        @Test
        @DisplayName("Should reject when available stock is null")
        void testNullStock() {
            assertThatThrownBy(() -> ValidationUtils.validateStock(5, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("Should show available stock in error message")
        void testErrorMessageContainsAvailableStock() {
            assertThatThrownBy(() -> ValidationUtils.validateStock(10, 3))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Available: 3");
        }
    }
}