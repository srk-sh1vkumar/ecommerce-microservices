package com.ecommerce.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Feign client for notification-service integration.
 * Provides email notification capabilities for user-related events.
 *
 * <p>This client communicates with the notification-service microservice
 * to send various types of emails such as welcome emails, password reset
 * notifications, and account verification messages.
 *
 * <p>Features:
 * <ul>
 *   <li>Welcome email on user registration</li>
 *   <li>Password reset email with secure link</li>
 *   <li>Account verification email</li>
 *   <li>Automatic service discovery via Eureka</li>
 *   <li>Circuit breaker support for resilience</li>
 * </ul>
 *
 * @author E-commerce Development Team
 * @version 1.0
 * @since 1.0
 */
@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    /**
     * Sends a welcome email to a newly registered user.
     *
     * <p>The welcome email includes:
     * <ul>
     *   <li>Personalized greeting with user's name</li>
     *   <li>Account activation link</li>
     *   <li>Platform features overview</li>
     *   <li>Getting started information</li>
     * </ul>
     *
     * @param email User's email address
     * @param name User's full name for personalization
     * @param activationLink Unique link for account activation
     * @return ResponseEntity containing success/failure status
     * @throws feign.FeignException if notification service is unavailable
     *
     * @apiNote This method is called asynchronously after user registration
     * to avoid blocking the registration response.
     *
     * @implNote Email sending is non-blocking on the notification service side.
     * Failures are logged but don't affect the registration process.
     */
    @PostMapping("/api/notifications/welcome")
    ResponseEntity<Map<String, String>> sendWelcomeEmail(
            @RequestParam("email") String email,
            @RequestParam("name") String name,
            @RequestParam("activationLink") String activationLink
    );

    /**
     * Sends a password reset email with a secure reset link.
     *
     * <p>The password reset email includes:
     * <ul>
     *   <li>Personalized greeting</li>
     *   <li>Secure password reset link (time-limited)</li>
     *   <li>Security warnings and best practices</li>
     *   <li>Link expiry information</li>
     * </ul>
     *
     * @param email User's email address
     * @param name User's full name
     * @param resetLink Secure, time-limited password reset link
     * @return ResponseEntity containing success/failure status
     * @throws feign.FeignException if notification service is unavailable
     *
     * @apiNote Reset links should expire within 24 hours for security.
     *
     * @implNote The notification service validates that the email format
     * is correct before attempting to send.
     */
    @PostMapping("/api/notifications/password-reset")
    ResponseEntity<Map<String, String>> sendPasswordResetEmail(
            @RequestParam("email") String email,
            @RequestParam("name") String name,
            @RequestParam("resetLink") String resetLink
    );
}
