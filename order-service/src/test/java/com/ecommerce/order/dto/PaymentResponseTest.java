package com.ecommerce.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PaymentResponse DTO.
 * Tests DTO builder and getters.
 */
@DisplayName("PaymentResponse DTO Tests")
class PaymentResponseTest {

    @Test
    @DisplayName("Builder - Should create DTO with all fields")
    void builder_ShouldCreateDTOWithAllFields() {
        // Act
        PaymentResponse response = PaymentResponse.builder()
            .transactionId("pi_test123")
            .status("succeeded")
            .amount(new BigDecimal("99.99"))
            .success(true)
            .errorMessage(null)
            .build();

        // Assert
        assertThat(response.getTransactionId()).isEqualTo("pi_test123");
        assertThat(response.getStatus()).isEqualTo("succeeded");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Builder - Should create error response")
    void builder_ShouldCreateErrorResponse() {
        // Act
        PaymentResponse response = PaymentResponse.builder()
            .success(false)
            .errorMessage("Payment declined")
            .build();

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("Payment declined");
        assertThat(response.getTransactionId()).isNull();
    }

    @Test
    @DisplayName("IsSuccess - Should return correct boolean value")
    void isSuccess_ShouldReturnCorrectValue() {
        // Arrange
        PaymentResponse successResponse = PaymentResponse.builder()
            .success(true)
            .build();

        PaymentResponse failureResponse = PaymentResponse.builder()
            .success(false)
            .build();

        // Assert
        assertThat(successResponse.isSuccess()).isTrue();
        assertThat(failureResponse.isSuccess()).isFalse();
    }
}
