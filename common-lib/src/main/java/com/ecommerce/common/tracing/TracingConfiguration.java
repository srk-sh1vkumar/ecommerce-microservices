package com.ecommerce.common.tracing;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enhanced distributed tracing configuration.
 *
 * Features:
 * - Custom span attributes
 * - Business context propagation
 * - Performance tracking
 * - Error tracking with stack traces
 *
 * Integration:
 * - OpenTelemetry compatible
 * - Jaeger backend support
 * - Zipkin backend support
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Configuration
public class TracingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TracingConfiguration.class);

    @Bean
    public TracingHelper tracingHelper(Tracer tracer) {
        logger.info("Initializing enhanced distributed tracing");
        return new TracingHelper(tracer);
    }
}