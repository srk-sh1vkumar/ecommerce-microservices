package com.ecommerce.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP aspect for structured logging across all services.
 * Automatically logs method entry, exit, exceptions, and execution time.
 */
@Aspect
@Component
public class LoggingAspect {

    /**
     * Logs execution of service layer methods with timing and parameters.
     */
    @Around("execution(* com.ecommerce.*.service.*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }

    /**
     * Logs execution of controller layer methods.
     */
    @Around("execution(* com.ecommerce.*.controller.*.*(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "CONTROLLER");
    }

    /**
     * Logs execution of repository layer methods.
     */
    @Around("execution(* com.ecommerce.*.repository.*.*(..))")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "REPOSITORY");
    }

    /**
     * Core logging logic for method execution.
     */
    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        Logger logger = LoggerFactory.getLogger(signature.getDeclaringType());

        // Add layer to MDC
        MDC.put("layer", layer);

        long startTime = System.currentTimeMillis();

        try {
            // Log method entry
            if (logger.isDebugEnabled()) {
                Object[] args = joinPoint.getArgs();
                logger.debug("{}.{} - Entry - Args: {}",
                        className, methodName, Arrays.toString(args));
            }

            // Execute method
            Object result = joinPoint.proceed();

            // Log method exit with execution time
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("{}.{} - Success - Time: {}ms",
                    className, methodName, executionTime);

            // Log slow methods as warnings
            if (executionTime > 1000) {
                logger.warn("{}.{} - SLOW EXECUTION - Time: {}ms",
                        className, methodName, executionTime);
            }

            return result;

        } catch (Exception e) {
            // Log method exception
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("{}.{} - Exception - Time: {}ms - Error: {}",
                    className, methodName, executionTime, e.getMessage(), e);
            throw e;

        } finally {
            MDC.remove("layer");
        }
    }
}
