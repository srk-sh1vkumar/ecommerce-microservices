package com.ecommerce.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for email notification requests.
 *
 * <p>This class encapsulates all information needed to send an email notification.</p>
 *
 * @author E-commerce Development Team
 * @version 1.0
 */
public class EmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotNull(message = "Email type is required")
    private EmailType emailType;

    /**
     * Template variables for personalization.
     * e.g., {"userName": "John", "orderId": "12345"}
     */
    private Map<String, Object> templateData = new HashMap<>();

    public EmailRequest() {
    }

    public EmailRequest(String to, String subject, EmailType emailType) {
        this.to = to;
        this.subject = subject;
        this.emailType = emailType;
    }

    // Getters and Setters
    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public EmailType getEmailType() {
        return emailType;
    }

    public void setEmailType(EmailType emailType) {
        this.emailType = emailType;
    }

    public Map<String, Object> getTemplateData() {
        return templateData;
    }

    public void setTemplateData(Map<String, Object> templateData) {
        this.templateData = templateData;
    }

    public void addTemplateData(String key, Object value) {
        this.templateData.put(key, value);
    }

    /**
     * Enum defining supported email types.
     */
    public enum EmailType {
        WELCOME,
        ORDER_CONFIRMATION,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        PASSWORD_RESET,
        ACCOUNT_VERIFICATION,
        PROMOTIONAL
    }
}
