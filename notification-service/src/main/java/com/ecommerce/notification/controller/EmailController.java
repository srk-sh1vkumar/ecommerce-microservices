package com.ecommerce.notification.controller;

import com.ecommerce.notification.dto.EmailRequest;
import com.ecommerce.notification.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for email notification operations.
 *
 * <p>Provides endpoints for sending various types of transactional emails.</p>
 *
 * @author E-commerce Development Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Email Notifications", description = "Email notification management APIs")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Operation(
            summary = "Send email notification",
            description = "Sends an email notification using the specified template and data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Email sent successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "500", description = "Email sending failed")
            }
    )
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendEmail(@Valid @RequestBody EmailRequest emailRequest) {
        emailService.sendEmail(emailRequest);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Email sent successfully to " + emailRequest.getTo()
        ));
    }

    @Operation(
            summary = "Send welcome email",
            description = "Sends a welcome email to a newly registered user"
    )
    @PostMapping("/welcome")
    public ResponseEntity<Map<String, String>> sendWelcomeEmail(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam String activationLink) {

        emailService.sendWelcomeEmail(email, name, activationLink);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Welcome email sent to " + email
        ));
    }

    @Operation(
            summary = "Send order confirmation email",
            description = "Sends an order confirmation email with order details"
    )
    @PostMapping("/order-confirmation")
    public ResponseEntity<Map<String, String>> sendOrderConfirmation(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam String orderId,
            @RequestParam String orderTotal,
            @RequestBody(required = false) Object orderItems) {

        emailService.sendOrderConfirmationEmail(email, name, orderId, orderTotal, orderItems);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Order confirmation sent to " + email
        ));
    }

    @Operation(
            summary = "Send password reset email",
            description = "Sends a password reset email with reset link"
    )
    @PostMapping("/password-reset")
    public ResponseEntity<Map<String, String>> sendPasswordReset(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam String resetLink) {

        emailService.sendPasswordResetEmail(email, name, resetLink);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Password reset email sent to " + email
        ));
    }

    @Operation(
            summary = "Send order shipped notification",
            description = "Sends an email when an order has been shipped"
    )
    @PostMapping("/order-shipped")
    public ResponseEntity<Map<String, String>> sendOrderShipped(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam String orderId,
            @RequestParam String trackingNumber,
            @RequestParam String carrier) {

        emailService.sendOrderShippedEmail(email, name, orderId, trackingNumber, carrier);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Order shipped notification sent to " + email
        ));
    }

    @Operation(
            summary = "Health check",
            description = "Simple health check endpoint"
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "notification-service"
        ));
    }
}
