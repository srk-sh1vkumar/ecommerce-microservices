package com.ecommerce.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for automatic metrics collection.
 * Tracks method execution times and error rates.
 */
@Aspect
@Component
public class MetricsAspect {

    private final MeterRegistry meterRegistry;

    public MetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Tracks execution time of all service methods.
     */
    @Around("execution(* com.ecommerce.*.service.*.*(..))")
    public Object trackServiceMethodMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();

            // Record successful execution
            sample.stop(Timer.builder("service.method.execution")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", "success")
                    .description("Service method execution time")
                    .register(meterRegistry));

            return result;

        } catch (Exception e) {
            // Record failed execution
            sample.stop(Timer.builder("service.method.execution")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", "error")
                    .tag("exception", e.getClass().getSimpleName())
                    .description("Service method execution time")
                    .register(meterRegistry));

            // Increment error counter
            meterRegistry.counter("service.method.errors",
                    "class", className,
                    "method", methodName,
                    "exception", e.getClass().getSimpleName()).increment();

            throw e;
        }
    }

    /**
     * Tracks controller endpoint metrics.
     */
    @Around("execution(* com.ecommerce.*.controller.*.*(..))")
    public Object trackControllerMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();

            sample.stop(Timer.builder("controller.endpoint.execution")
                    .tag("controller", className)
                    .tag("endpoint", methodName)
                    .tag("status", "success")
                    .description("Controller endpoint execution time")
                    .register(meterRegistry));

            return result;

        } catch (Exception e) {
            sample.stop(Timer.builder("controller.endpoint.execution")
                    .tag("controller", className)
                    .tag("endpoint", methodName)
                    .tag("status", "error")
                    .description("Controller endpoint execution time")
                    .register(meterRegistry));

            meterRegistry.counter("controller.endpoint.errors",
                    "controller", className,
                    "endpoint", methodName).increment();

            throw e;
        }
    }
}
