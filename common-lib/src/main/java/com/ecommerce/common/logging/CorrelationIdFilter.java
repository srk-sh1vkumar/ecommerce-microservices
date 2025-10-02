package com.ecommerce.common.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that ensures every HTTP request has a correlation ID for distributed tracing.
 * Correlation IDs are propagated across microservices for end-to-end request tracking.
 *
 * Features:
 * - Generates unique correlation ID if not present
 * - Extracts correlation ID from incoming requests
 * - Adds correlation ID to response headers
 * - Stores correlation ID in MDC for logging
 * - Cleans up MDC after request processing
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Try to get correlation ID from request header
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);

            // Generate new correlation ID if not present
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = generateCorrelationId();
                logger.debug("Generated new correlation ID: {}", correlationId);
            } else {
                logger.debug("Using existing correlation ID: {}", correlationId);
            }

            // Store in MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Add to response header for downstream services
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue with request processing
            chain.doFilter(request, response);

        } finally {
            // Clean up MDC after request
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * Generates a unique correlation ID.
     *
     * @return UUID-based correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("CorrelationIdFilter initialized");
    }

    @Override
    public void destroy() {
        logger.info("CorrelationIdFilter destroyed");
    }
}
