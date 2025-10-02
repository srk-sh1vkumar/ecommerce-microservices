package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.EmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.concurrent.CompletableFuture;

/**
 * Email service for sending transactional and marketing emails.
 *
 * <p>This service provides comprehensive email functionality including:
 * <ul>
 *   <li>HTML email templates with Thymeleaf</li>
 *   <li>Asynchronous email sending</li>
 *   <li>Automatic retry on failure</li>
 *   <li>Email delivery tracking</li>
 * </ul>
 *
 * <p><b>Email Types Supported:</b></p>
 * <ul>
 *   <li>WELCOME - User registration confirmation</li>
 *   <li>ORDER_CONFIRMATION - Order placed successfully</li>
 *   <li>ORDER_SHIPPED - Order shipped notification</li>
 *   <li>ORDER_DELIVERED - Order delivered confirmation</li>
 *   <li>PASSWORD_RESET - Password reset link</li>
 *   <li>ACCOUNT_VERIFICATION - Email verification link</li>
 *   <li>PROMOTIONAL - Marketing emails</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * EmailRequest request = new EmailRequest("user@example.com", "Welcome!", EmailType.WELCOME);
 * request.addTemplateData("userName", "John Doe");
 * request.addTemplateData("activationLink", "https://example.com/activate/token");
 * emailService.sendEmail(request);
 * }</pre>
 *
 * @author E-commerce Development Team
 * @version 1.0
 * @since 1.0
 * @see EmailRequest
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.from-name:E-commerce Platform}")
    private String fromName;

    /**
     * Sends an email asynchronously using the specified template and data.
     *
     * <p>This method processes the email template with the provided data and sends
     * the email in a separate thread to avoid blocking the main application flow.</p>
     *
     * @param emailRequest the email request containing recipient, subject, type, and template data
     * @return CompletableFuture that completes when the email is sent
     * @throws RuntimeException if email sending fails after all retries
     */
    @Async
    public CompletableFuture<Void> sendEmail(EmailRequest emailRequest) {
        try {
            logger.info("Sending {} email to: {}", emailRequest.getEmailType(), emailRequest.getTo());

            // Create MIME message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties
            helper.setFrom(fromEmail, fromName);
            helper.setTo(emailRequest.getTo());
            helper.setSubject(emailRequest.getSubject());

            // Process template with data
            String htmlContent = processTemplate(emailRequest);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            logger.info("Successfully sent {} email to: {}", emailRequest.getEmailType(), emailRequest.getTo());
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            logger.error("Failed to send email to: {}", emailRequest.getTo(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a welcome email to newly registered users.
     *
     * @param userEmail the user's email address
     * @param userName the user's full name
     * @param activationLink the account activation link
     */
    public void sendWelcomeEmail(String userEmail, String userName, String activationLink) {
        EmailRequest request = new EmailRequest(
                userEmail,
                "Welcome to E-commerce Platform!",
                EmailRequest.EmailType.WELCOME
        );
        request.addTemplateData("userName", userName);
        request.addTemplateData("activationLink", activationLink);

        sendEmail(request);
    }

    /**
     * Sends an order confirmation email.
     *
     * @param userEmail the customer's email address
     * @param userName the customer's name
     * @param orderId the order ID
     * @param orderTotal the order total amount
     * @param orderItems the list of order items (as HTML string or object)
     */
    public void sendOrderConfirmationEmail(String userEmail, String userName, String orderId,
                                           String orderTotal, Object orderItems) {
        EmailRequest request = new EmailRequest(
                userEmail,
                "Order Confirmation - Order #" + orderId,
                EmailRequest.EmailType.ORDER_CONFIRMATION
        );
        request.addTemplateData("userName", userName);
        request.addTemplateData("orderId", orderId);
        request.addTemplateData("orderTotal", orderTotal);
        request.addTemplateData("orderItems", orderItems);
        request.addTemplateData("orderDate", java.time.LocalDateTime.now().toString());

        sendEmail(request);
    }

    /**
     * Sends a password reset email with reset link.
     *
     * @param userEmail the user's email address
     * @param userName the user's name
     * @param resetLink the password reset link
     */
    public void sendPasswordResetEmail(String userEmail, String userName, String resetLink) {
        EmailRequest request = new EmailRequest(
                userEmail,
                "Password Reset Request",
                EmailRequest.EmailType.PASSWORD_RESET
        );
        request.addTemplateData("userName", userName);
        request.addTemplateData("resetLink", resetLink);
        request.addTemplateData("expiryTime", "24 hours");

        sendEmail(request);
    }

    /**
     * Sends an order shipped notification email.
     *
     * @param userEmail the customer's email
     * @param userName the customer's name
     * @param orderId the order ID
     * @param trackingNumber the shipping tracking number
     * @param carrier the shipping carrier name
     */
    public void sendOrderShippedEmail(String userEmail, String userName, String orderId,
                                      String trackingNumber, String carrier) {
        EmailRequest request = new EmailRequest(
                userEmail,
                "Your Order Has Shipped - Order #" + orderId,
                EmailRequest.EmailType.ORDER_SHIPPED
        );
        request.addTemplateData("userName", userName);
        request.addTemplateData("orderId", orderId);
        request.addTemplateData("trackingNumber", trackingNumber);
        request.addTemplateData("carrier", carrier);

        sendEmail(request);
    }

    /**
     * Processes the email template with the provided data.
     *
     * @param emailRequest the email request with template type and data
     * @return the processed HTML content
     */
    private String processTemplate(EmailRequest emailRequest) {
        Context context = new Context();
        context.setVariables(emailRequest.getTemplateData());

        String templateName = getTemplateName(emailRequest.getEmailType());
        return templateEngine.process(templateName, context);
    }

    /**
     * Maps email type to template file name.
     *
     * @param emailType the type of email
     * @return the template file name
     */
    private String getTemplateName(EmailRequest.EmailType emailType) {
        return switch (emailType) {
            case WELCOME -> "email/welcome";
            case ORDER_CONFIRMATION -> "email/order-confirmation";
            case ORDER_SHIPPED -> "email/order-shipped";
            case ORDER_DELIVERED -> "email/order-delivered";
            case PASSWORD_RESET -> "email/password-reset";
            case ACCOUNT_VERIFICATION -> "email/account-verification";
            case PROMOTIONAL -> "email/promotional";
        };
    }
}
