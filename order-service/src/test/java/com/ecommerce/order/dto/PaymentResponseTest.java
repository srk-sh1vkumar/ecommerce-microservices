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

    @Test
    @DisplayName("Builder - Should create DTO with currency and orderId")
    void builder_ShouldCreateDTOWithCurrencyAndOrderId() {
        // Act
        PaymentResponse response = PaymentResponse.builder()
            .transactionId("pi_test456")
            .status("processing")
            .amount(new BigDecimal("249.99"))
            .currency("USD")
            .orderId("order789")
            .success(true)
            .build();

        // Assert
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getOrderId()).isEqualTo("order789");
    }

    @Test
    @DisplayName("Default constructor - Should create empty object")
    void defaultConstructor_ShouldCreateEmptyObject() {
        // Act
        PaymentResponse response = new PaymentResponse();

        // Assert
        assertThat(response.getTransactionId()).isNull();
        assertThat(response.getStatus()).isNull();
        assertThat(response.getAmount()).isNull();
        assertThat(response.getCurrency()).isNull();
        assertThat(response.getOrderId()).isNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Setters - Should update all fields correctly")
    void setters_ShouldUpdateAllFields() {
        // Arrange
        PaymentResponse response = new PaymentResponse();

        // Act
        response.setTransactionId("pi_new123");
        response.setStatus("completed");
        response.setAmount(new BigDecimal("350.00"));
        response.setCurrency("EUR");
        response.setOrderId("order999");
        response.setSuccess(true);
        response.setErrorMessage("No errors");

        // Assert
        assertThat(response.getTransactionId()).isEqualTo("pi_new123");
        assertThat(response.getStatus()).isEqualTo("completed");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(response.getCurrency()).isEqualTo("EUR");
        assertThat(response.getOrderId()).isEqualTo("order999");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getErrorMessage()).isEqualTo("No errors");
    }
}
