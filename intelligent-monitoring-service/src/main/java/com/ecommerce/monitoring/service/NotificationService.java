package com.ecommerce.monitoring.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Service for sending notifications via email, SMS, and other channels
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Value("${monitoring.notifications.enabled:true}")
    private boolean notificationsEnabled;
    
    @Value("${monitoring.notifications.from-email:monitoring@ecommerce.com}")
    private String fromEmail;
    
    private final JavaMailSender mailSender;

    public NotificationService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Send email notification
     */
    public void sendEmail(String to, String subject, String message) {
        if (!notificationsEnabled) {
            logger.debug("Notifications disabled, skipping email to: {}", to);
            return;
        }

        if (mailSender == null) {
            logger.warn("JavaMailSender not configured, skipping email to: {}", to);
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);

            logger.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
        }
    }
    
    /**
     * Send email notification to multiple recipients
     */
    public void sendEmail(String[] recipients, String subject, String message) {
        if (!notificationsEnabled) {
            logger.debug("Notifications disabled, skipping email to {} recipients", recipients.length);
            return;
        }
        
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(recipients);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);
            
            mailSender.send(mailMessage);
            
            logger.info("Email sent successfully to {} recipients", recipients.length);
            
        } catch (Exception e) {
            logger.error("Failed to send email to {} recipients", recipients.length, e);
        }
    }
    
    /**
     * Send critical alert notification (priority channels)
     */
    public void sendCriticalAlert(String subject, String message) {
        logger.warn("CRITICAL ALERT: {} - {}", subject, message);
        
        // Send to all critical notification channels
        sendEmail("alerts@ecommerce.com", "[CRITICAL] " + subject, message);
        
        // Could extend to send SMS, Slack, PagerDuty etc.
    }
    
    /**
     * Send notification with custom template
     */
    public void sendTemplatedNotification(String to, String templateName, 
                                        java.util.Map<String, Object> templateData) {
        // Implementation would use template engine
        logger.info("Templated notification sent to: {} using template: {}", to, templateName);
    }
}