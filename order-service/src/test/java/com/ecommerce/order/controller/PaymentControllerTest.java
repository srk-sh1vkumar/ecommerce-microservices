package com.ecommerce.order.controller;

import com.ecommerce.order.dto.PaymentRequest;
import com.ecommerce.order.dto.PaymentResponse;
import com.ecommerce.order.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PaymentController.
 * Tests REST endpoints for payment operations.
 */
@WebMvcTest(PaymentController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@DisplayName("PaymentController Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private com.ecommerce.order.service.OrderService orderService;

    @MockBean
    private com.ecommerce.order.repository.OrderRepository orderRepository;

    @MockBean
    private com.ecommerce.order.client.CartServiceClient cartServiceClient;

    @MockBean
    private com.ecommerce.order.client.ProductServiceClient productServiceClient;

    @MockBean
    private com.ecommerce.order.client.NotificationServiceClient notificationServiceClient;

    @MockBean
    private com.ecommerce.common.metrics.MetricsService metricsService;

    private PaymentRequest paymentRequest;
    private PaymentResponse successResponse;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest(
            "order123",
            new BigDecimal("99.99"),
            "test@example.com"
        );

        successResponse = PaymentResponse.builder()
            .transactionId("pi_test123")
            .status("succeeded")
            .amount(new BigDecimal("99.99"))
            .success(true)
            .build();
    }

    @Test
    @DisplayName("Create Payment - Should return 200 with successful payment")
    void createPayment_WithValidRequest_ShouldReturn200() throws Exception {
        // Arrange
        doNothing().when(paymentService).validatePaymentRequest(any(PaymentRequest.class));
        when(paymentService.createPaymentIntent(any(PaymentRequest.class))).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").value("pi_test123"))
            .andExpect(jsonPath("$.status").value("succeeded"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.amount").value(99.99));

        verify(paymentService).validatePaymentRequest(any(PaymentRequest.class));
        verify(paymentService).createPaymentIntent(any(PaymentRequest.class));
    }

    @Test
    @DisplayName("Create Payment - Should return 400 when validation fails")
    void createPayment_WithInvalidRequest_ShouldReturn400() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Invalid payment amount"))
            .when(paymentService).validatePaymentRequest(any(PaymentRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/payments/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorMessage").value("Invalid payment amount"));

        verify(paymentService).validatePaymentRequest(any(PaymentRequest.class));
        verify(paymentService, never()).createPaymentIntent(any(PaymentRequest.class));
    }

    @Test
    @DisplayName("Create Payment - Should return 500 when payment processing fails")
    void createPayment_WhenProcessingFails_ShouldReturn500() throws Exception {
        // Arrange
        doNothing().when(paymentService).validatePaymentRequest(any(PaymentRequest.class));
        when(paymentService.createPaymentIntent(any(PaymentRequest.class)))
            .thenThrow(new RuntimeException("Stripe API error"));

        // Act & Assert
        mockMvc.perform(post("/api/payments/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorMessage").exists());
    }

    @Test
    @DisplayName("Get Payment Status - Should return 200 with status")
    void getPaymentStatus_WithValidId_ShouldReturn200() throws Exception {
        // Arrange
        when(paymentService.getPaymentStatus("pi_test123")).thenReturn("succeeded");

        // Act & Assert
        mockMvc.perform(get("/api/payments/status/pi_test123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentIntentId").value("pi_test123"))
            .andExpect(jsonPath("$.status").value("succeeded"));

        verify(paymentService).getPaymentStatus("pi_test123");
    }

    @Test
    @DisplayName("Get Payment Status - Should return 500 when retrieval fails")
    void getPaymentStatus_WhenRetrievalFails_ShouldReturn500() throws Exception {
        // Arrange
        when(paymentService.getPaymentStatus("invalid_id"))
            .thenThrow(new RuntimeException("Payment not found"));

        // Act & Assert
        mockMvc.perform(get("/api/payments/status/invalid_id"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Failed to retrieve payment status"));
    }

    @Test
    @DisplayName("Webhook - Should return 200 for valid signature")
    void handleWebhook_WithValidSignature_ShouldReturn200() throws Exception {
        // Arrange
        String payload = "{\"id\":\"evt_test\",\"type\":\"payment_intent.succeeded\"}";
        String signature = "valid_signature";

        // Act & Assert
        mockMvc.perform(post("/api/payments/webhook")
                .content(payload)
                .header("Stripe-Signature", signature))
            .andExpect(status().isOk())
            .andExpect(content().string("Webhook processed"));
    }

    @Test
    @DisplayName("Health Check - Should return 200")
    void health_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/payments/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("payment-service"));
    }
}
