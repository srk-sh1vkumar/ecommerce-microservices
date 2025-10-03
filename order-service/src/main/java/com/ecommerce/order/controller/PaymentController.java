package com.ecommerce.order.controller;

import com.ecommerce.order.dto.PaymentRequest;
import com.ecommerce.order.dto.PaymentResponse;
import com.ecommerce.order.service.PaymentService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for payment operations.
 * Handles payment creation, status checks, refunds, and Stripe webhooks.
 *
 * @author E-commerce Development Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "Payment processing APIs using Stripe")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Value("${stripe.webhook.secret:whsec_test_secret}")
    private String webhookSecret;

    /**
     * Creates a payment intent (used for direct payment creation).
     *
     * @param paymentRequest Payment details
     * @return Payment response with transaction ID
     */
    @PostMapping("/create")
    @Operation(summary = "Create payment intent", description = "Creates a new payment intent with Stripe")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        try {
            logger.info("Creating payment for order: {}", paymentRequest.getOrderId());
            paymentService.validatePaymentRequest(paymentRequest);

            PaymentResponse response = paymentService.createPaymentIntent(paymentRequest);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid payment request: {}", e.getMessage());
            PaymentResponse errorResponse = PaymentResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Payment creation failed: {}", e.getMessage());
            PaymentResponse errorResponse = PaymentResponse.builder()
                    .success(false)
                    .errorMessage("Payment processing failed: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves payment status by payment intent ID.
     *
     * @param paymentIntentId Stripe payment intent ID
     * @return Payment status
     */
    @GetMapping("/status/{paymentIntentId}")
    @Operation(summary = "Get payment status", description = "Retrieves the status of a payment")
    public ResponseEntity<Map<String, String>> getPaymentStatus(@PathVariable String paymentIntentId) {
        try {
            String status = paymentService.getPaymentStatus(paymentIntentId);
            return ResponseEntity.ok(Map.of(
                    "paymentIntentId", paymentIntentId,
                    "status", status
            ));
        } catch (Exception e) {
            logger.error("Failed to retrieve payment status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve payment status"));
        }
    }

    /**
     * Stripe webhook endpoint for payment events.
     * Handles asynchronous payment notifications from Stripe.
     *
     * @param payload Webhook payload from Stripe
     * @param signature Stripe signature header for verification
     * @return Status response
     */
    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook", description = "Handles Stripe webhook events")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        Event event;

        try {
            // Verify webhook signature
            event = Webhook.constructEvent(payload, signature, webhookSecret);
            logger.info("Received Stripe webhook event: {}", event.getType());

        } catch (Exception e) {
            logger.error("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // Handle different event types
        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentSucceeded(event);
                break;

            case "payment_intent.payment_failed":
                handlePaymentFailed(event);
                break;

            case "payment_intent.canceled":
                handlePaymentCanceled(event);
                break;

            case "charge.refunded":
                handleChargeRefunded(event);
                break;

            default:
                logger.info("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook processed");
    }

    /**
     * Handles successful payment events.
     *
     * @param event Stripe event
     */
    private void handlePaymentSucceeded(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);

        if (intent != null) {
            String orderId = intent.getMetadata().get("order_id");
            logger.info("Payment succeeded for order: {}. Transaction ID: {}",
                       orderId, intent.getId());

            // Update order status to CONFIRMED
            // This would trigger email notification, inventory updates, etc.
        }
    }

    /**
     * Handles failed payment events.
     *
     * @param event Stripe event
     */
    private void handlePaymentFailed(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);

        if (intent != null) {
            String orderId = intent.getMetadata().get("order_id");
            logger.error("Payment failed for order: {}. Reason: {}",
                        orderId, intent.getLastPaymentError());

            // Update order status to CANCELLED
            // Rollback stock
            // Send failure notification
        }
    }

    /**
     * Handles canceled payment events.
     *
     * @param event Stripe event
     */
    private void handlePaymentCanceled(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);

        if (intent != null) {
            String orderId = intent.getMetadata().get("order_id");
            logger.warn("Payment canceled for order: {}", orderId);

            // Update order status
            // Rollback stock
        }
    }

    /**
     * Handles refund events.
     *
     * @param event Stripe event
     */
    private void handleChargeRefunded(Event event) {
        logger.info("Charge refunded event received");
        // Handle refund confirmation
        // Update order status
        // Restore stock if applicable
    }

    /**
     * Health check endpoint.
     *
     * @return Status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "payment-service"
        ));
    }
}
