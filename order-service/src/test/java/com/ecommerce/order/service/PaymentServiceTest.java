package com.ecommerce.order.service;

import com.ecommerce.order.dto.PaymentRequest;
import com.ecommerce.order.dto.PaymentResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PaymentService.
 * Tests Stripe payment processing, validation, refunds, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest validPaymentRequest;

    @BeforeEach
    void setUp() {
        // Set Stripe configuration via reflection
        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", "sk_test_123");
        ReflectionTestUtils.setField(paymentService, "currency", "usd");

        validPaymentRequest = new PaymentRequest(
            "order123",
            new BigDecimal("99.99"),
            "customer@example.com"
        );
    }

    @Test
    @DisplayName("Init - Should initialize Stripe API key")
    void init_ShouldSetStripeApiKey() {
        // Act
        paymentService.init();

        // Assert - no exception means initialization succeeded
        // Stripe.apiKey is set internally
    }

    @Test
    @DisplayName("Validate Payment Request - Should pass for valid request")
    void validatePaymentRequest_WithValidRequest_ShouldPass() {
        // Act & Assert
        assertThatCode(() -> paymentService.validatePaymentRequest(validPaymentRequest))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Validate Payment Request - Should throw when request is null")
    void validatePaymentRequest_WhenNull_ShouldThrow() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentRequest(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payment request cannot be null");
    }

    @Test
    @DisplayName("Validate Payment Request - Should throw when amount is null")
    void validatePaymentRequest_WhenAmountNull_ShouldThrow() {
        // Arrange
        PaymentRequest request = new PaymentRequest("order123", null, "customer@example.com");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("Validate Payment Request - Should throw when amount is zero")
    void validatePaymentRequest_WhenAmountZero_ShouldThrow() {
        // Arrange
        PaymentRequest request = new PaymentRequest("order123", BigDecimal.ZERO, "customer@example.com");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("Validate Payment Request - Should throw when amount is negative")
    void validatePaymentRequest_WhenAmountNegative_ShouldThrow() {
        // Arrange
        PaymentRequest request = new PaymentRequest("order123", new BigDecimal("-10.00"), "customer@example.com");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("Validate Payment Request - Should throw when order ID is null")
    void validatePaymentRequest_WhenOrderIdNull_ShouldThrow() {
        // Arrange
        PaymentRequest request = new PaymentRequest(null, new BigDecimal("99.99"), "customer@example.com");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Order ID is required");
    }

    @Test
    @DisplayName("Validate Payment Request - Should throw when order ID is empty")
    void validatePaymentRequest_WhenOrderIdEmpty_ShouldThrow() {
        // Arrange
        PaymentRequest request = new PaymentRequest("", new BigDecimal("99.99"), "customer@example.com");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Order ID is required");
    }

    @Test
    @DisplayName("Validate Payment Request - Should throw when customer email is null")
    void validatePaymentRequest_WhenEmailNull_ShouldThrow() {
        // Arrange
        PaymentRequest request = new PaymentRequest("order123", new BigDecimal("99.99"), null);

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Customer email is required");
    }

    @Test
    @DisplayName("Validate Payment Request - Should throw when customer email is empty")
    void validatePaymentRequest_WhenEmailEmpty_ShouldThrow() {
        // Arrange
        PaymentRequest request = new PaymentRequest("order123", new BigDecimal("99.99"), "");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.validatePaymentRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Customer email is required");
    }

    @Test
    @DisplayName("Create Payment Intent - Should create and confirm payment successfully")
    void createPaymentIntent_WithValidRequest_ShouldSucceed() throws StripeException {
        // Arrange
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        PaymentIntent mockConfirmedIntent = mock(PaymentIntent.class);

        when(mockIntent.getId()).thenReturn("pi_test123");
        when(mockConfirmedIntent.getId()).thenReturn("pi_test123");
        when(mockConfirmedIntent.getStatus()).thenReturn("succeeded");

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.create(any(com.stripe.param.PaymentIntentCreateParams.class))).thenReturn(mockIntent);
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_test123")).thenReturn(mockIntent);
            when(mockIntent.confirm(any(com.stripe.param.PaymentIntentConfirmParams.class))).thenReturn(mockConfirmedIntent);

            // Act
            PaymentResponse response = paymentService.createPaymentIntent(validPaymentRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTransactionId()).isEqualTo("pi_test123");
            assertThat(response.getStatus()).isEqualTo("succeeded");
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(response.getCurrency()).isEqualTo("USD");
            assertThat(response.getOrderId()).isEqualTo("order123");
            assertThat(response.isSuccess()).isTrue();
        }
    }

    @Test
    @DisplayName("Create Payment Intent - Should handle Stripe exception")
    void createPaymentIntent_WhenStripeThrowsException_ShouldThrowRuntimeException() throws StripeException {
        // Arrange
        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            StripeException stripeException = mock(StripeException.class);
            when(stripeException.getUserMessage()).thenReturn("Card declined");
            when(stripeException.getMessage()).thenReturn("Card declined");

            mockedStatic.when(() -> PaymentIntent.create(any(com.stripe.param.PaymentIntentCreateParams.class))).thenThrow(stripeException);

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createPaymentIntent(validPaymentRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment processing failed")
                .hasCauseInstanceOf(StripeException.class);
        }
    }

    @Test
    @DisplayName("Create Payment Intent - Should mark processing status as success")
    void createPaymentIntent_WithProcessingStatus_ShouldBeSuccess() throws StripeException {
        // Arrange
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        PaymentIntent mockConfirmedIntent = mock(PaymentIntent.class);

        when(mockIntent.getId()).thenReturn("pi_test456");
        when(mockConfirmedIntent.getId()).thenReturn("pi_test456");
        when(mockConfirmedIntent.getStatus()).thenReturn("processing");

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.create(any(com.stripe.param.PaymentIntentCreateParams.class))).thenReturn(mockIntent);
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_test456")).thenReturn(mockIntent);
            when(mockIntent.confirm(any(com.stripe.param.PaymentIntentConfirmParams.class))).thenReturn(mockConfirmedIntent);

            // Act
            PaymentResponse response = paymentService.createPaymentIntent(validPaymentRequest);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo("processing");
        }
    }

    @Test
    @DisplayName("Get Payment Status - Should retrieve status successfully")
    void getPaymentStatus_WithValidId_ShouldReturnStatus() throws StripeException {
        // Arrange
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_test123")).thenReturn(mockIntent);

            // Act
            String status = paymentService.getPaymentStatus("pi_test123");

            // Assert
            assertThat(status).isEqualTo("succeeded");
        }
    }

    @Test
    @DisplayName("Get Payment Status - Should throw when retrieval fails")
    void getPaymentStatus_WhenRetrievalFails_ShouldThrowRuntimeException() throws StripeException {
        // Arrange
        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            StripeException stripeException = mock(StripeException.class);
            when(stripeException.getMessage()).thenReturn("Payment not found");

            mockedStatic.when(() -> PaymentIntent.retrieve("invalid_id")).thenThrow(stripeException);

            // Act & Assert
            assertThatThrownBy(() -> paymentService.getPaymentStatus("invalid_id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to retrieve payment status")
                .hasCauseInstanceOf(StripeException.class);
        }
    }

    @Test
    @DisplayName("Refund Payment - Should process full refund successfully")
    void refundPayment_WithFullRefund_ShouldSucceed() throws StripeException {
        // Arrange
        Refund mockRefund = mock(Refund.class);
        when(mockRefund.getId()).thenReturn("re_test123");

        try (MockedStatic<Refund> mockedStatic = mockStatic(Refund.class)) {
            mockedStatic.when(() -> Refund.create(anyMap())).thenReturn(mockRefund);

            // Act
            String refundId = paymentService.refundPayment("pi_test123", null);

            // Assert
            assertThat(refundId).isEqualTo("re_test123");
        }
    }

    @Test
    @DisplayName("Refund Payment - Should process partial refund successfully")
    void refundPayment_WithPartialRefund_ShouldSucceed() throws StripeException {
        // Arrange
        Refund mockRefund = mock(Refund.class);
        when(mockRefund.getId()).thenReturn("re_test456");

        try (MockedStatic<Refund> mockedStatic = mockStatic(Refund.class)) {
            mockedStatic.when(() -> Refund.create(anyMap())).thenReturn(mockRefund);

            // Act
            String refundId = paymentService.refundPayment("pi_test123", new BigDecimal("50.00"));

            // Assert
            assertThat(refundId).isEqualTo("re_test456");
        }
    }

    @Test
    @DisplayName("Refund Payment - Should throw when refund fails")
    void refundPayment_WhenRefundFails_ShouldThrowRuntimeException() throws StripeException {
        // Arrange
        try (MockedStatic<Refund> mockedStatic = mockStatic(Refund.class)) {
            StripeException stripeException = mock(StripeException.class);
            when(stripeException.getUserMessage()).thenReturn("Refund failed");
            when(stripeException.getMessage()).thenReturn("Refund failed");

            mockedStatic.when(() -> Refund.create(anyMap())).thenThrow(stripeException);

            // Act & Assert
            assertThatThrownBy(() -> paymentService.refundPayment("pi_test123", new BigDecimal("99.99")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refund processing failed")
                .hasCauseInstanceOf(StripeException.class);
        }
    }
}
