package com.ecommerce.order.controller;

import com.ecommerce.order.dto.PaymentRequest;
import com.ecommerce.order.dto.PaymentResponse;
import com.ecommerce.order.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone unit tests for PaymentController using MockMvc without Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Standalone Tests")
class PaymentControllerStandaloneTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private PaymentRequest paymentRequest;
    private PaymentResponse successResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
        objectMapper = new ObjectMapper();

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
    @DisplayName("Health Check - Should return 200")
    void health_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/payments/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("payment-service"));
    }
}
