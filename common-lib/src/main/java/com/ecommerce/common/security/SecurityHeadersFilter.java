package com.ecommerce.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security headers filter to protect against common web vulnerabilities.
 *
 * Implemented Security Headers:
 * - Content-Security-Policy: Prevent XSS attacks
 * - X-Content-Type-Options: Prevent MIME sniffing
 * - X-Frame-Options: Prevent clickjacking
 * - X-XSS-Protection: Browser XSS protection
 * - Strict-Transport-Security: Force HTTPS
 * - Referrer-Policy: Control referrer information
 * - Permissions-Policy: Control browser features
 *
 * Security Standards:
 * - OWASP Secure Headers Project compliant
 * - Mozilla Observatory A+ rating configuration
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP_POLICY =
        "default-src 'self'; " +
        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
        "img-src 'self' data: https:; " +
        "font-src 'self' data: https://fonts.gstatic.com; " +
        "connect-src 'self' https://api.ecommerce.com; " +
        "frame-ancestors 'none'; " +
        "base-uri 'self'; " +
        "form-action 'self';";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Content Security Policy - Prevent XSS attacks
        response.setHeader("Content-Security-Policy", CSP_POLICY);

        // X-Content-Type-Options - Prevent MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // X-Frame-Options - Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // X-XSS-Protection - Enable browser XSS protection
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Strict-Transport-Security - Force HTTPS (31536000 = 1 year)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // Referrer-Policy - Control referrer information
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions-Policy - Control browser features
        response.setHeader("Permissions-Policy",
            "geolocation=(), " +
            "microphone=(), " +
            "camera=(), " +
            "payment=(), " +
            "usb=(), " +
            "magnetometer=(), " +
            "gyroscope=(), " +
            "accelerometer=()");

        // Remove server header to prevent information disclosure
        response.setHeader("Server", "");

        // Cache control for sensitive data
        if (request.getRequestURI().contains("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        filterChain.doFilter(request, response);
    }
}