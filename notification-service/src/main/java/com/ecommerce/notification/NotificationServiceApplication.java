package com.ecommerce.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Notification Service - Email notification microservice for e-commerce platform.
 *
 * <p>This service handles all email communications including:
 * <ul>
 *   <li>User registration welcome emails</li>
 *   <li>Order confirmation emails</li>
 *   <li>Order status updates</li>
 *   <li>Password reset emails</li>
 *   <li>Promotional emails</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>HTML email templates with Thymeleaf</li>
 *   <li>Async email sending</li>
 *   <li>Event-driven notifications via Kafka</li>
 *   <li>Email delivery tracking</li>
 *   <li>Retry mechanism for failed emails</li>
 * </ul>
 *
 * @author E-commerce Development Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableKafka
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
