package com.ecommerce.common.constants;

/**
 * Application-wide error codes for consistent error handling across services.
 * These codes help clients handle specific error scenarios programmatically.
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
public final class ErrorCodes {

    private ErrorCodes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // User Service Errors (1xxx)
    public static final String USER_NOT_FOUND = "USR-1001";
    public static final String USER_ALREADY_EXISTS = "USR-1002";
    public static final String INVALID_CREDENTIALS = "USR-1003";
    public static final String WEAK_PASSWORD = "USR-1004";
    public static final String INVALID_EMAIL = "USR-1005";

    // Product Service Errors (2xxx)
    public static final String PRODUCT_NOT_FOUND = "PRD-2001";
    public static final String INSUFFICIENT_STOCK = "PRD-2002";
    public static final String INVALID_PRICE = "PRD-2003";
    public static final String INVALID_CATEGORY = "PRD-2004";
    public static final String PRODUCT_OUT_OF_STOCK = "PRD-2005";

    // Cart Service Errors (3xxx)
    public static final String CART_EMPTY = "CRT-3001";
    public static final String CART_ITEM_NOT_FOUND = "CRT-3002";
    public static final String INVALID_QUANTITY = "CRT-3003";
    public static final String CART_LIMIT_EXCEEDED = "CRT-3004";

    // Order Service Errors (4xxx)
    public static final String ORDER_NOT_FOUND = "ORD-4001";
    public static final String ORDER_ALREADY_PROCESSED = "ORD-4002";
    public static final String INVALID_SHIPPING_ADDRESS = "ORD-4003";
    public static final String PAYMENT_FAILED = "ORD-4004";
    public static final String ORDER_CANNOT_BE_CANCELLED = "ORD-4005";

    // General Errors (9xxx)
    public static final String VALIDATION_ERROR = "GEN-9001";
    public static final String INTERNAL_ERROR = "GEN-9002";
    public static final String SERVICE_UNAVAILABLE = "GEN-9003";
    public static final String DATABASE_ERROR = "GEN-9004";
    public static final String EXTERNAL_SERVICE_ERROR = "GEN-9005";
}