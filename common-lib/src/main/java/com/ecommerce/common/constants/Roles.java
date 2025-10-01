package com.ecommerce.common.constants;

/**
 * Centralized role definitions for the e-commerce platform.
 * Used for RBAC (Role-Based Access Control) across all services.
 */
public final class Roles {

    private Roles() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Role names (without ROLE_ prefix)
    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String CUSTOMER_SERVICE = "CUSTOMER_SERVICE";

    // Full role names with ROLE_ prefix (for Spring Security)
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";
    public static final String ROLE_CUSTOMER_SERVICE = "ROLE_CUSTOMER_SERVICE";

    // SpEL expressions for @PreAuthorize (most commonly used)
    public static final String HAS_ROLE_USER = "hasRole('USER')";
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_ROLE_MANAGER = "hasRole('MANAGER')";
    public static final String HAS_ROLE_CUSTOMER_SERVICE = "hasRole('CUSTOMER_SERVICE')";

    // Combined expressions
    public static final String HAS_ADMIN_OR_MANAGER = "hasAnyRole('ADMIN', 'MANAGER')";
    public static final String HAS_ADMIN_OR_CUSTOMER_SERVICE = "hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')";
    public static final String IS_AUTHENTICATED = "isAuthenticated()";

    /**
     * Checks if a role string is valid.
     */
    public static boolean isValidRole(String role) {
        return USER.equals(role) ||
               ADMIN.equals(role) ||
               MANAGER.equals(role) ||
               CUSTOMER_SERVICE.equals(role);
    }

    /**
     * Adds ROLE_ prefix if not present.
     */
    public static String withPrefix(String role) {
        if (role == null || role.isEmpty()) {
            return role;
        }
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    /**
     * Removes ROLE_ prefix if present.
     */
    public static String withoutPrefix(String role) {
        if (role == null || role.isEmpty()) {
            return role;
        }
        return role.startsWith("ROLE_") ? role.substring(5) : role;
    }
}
