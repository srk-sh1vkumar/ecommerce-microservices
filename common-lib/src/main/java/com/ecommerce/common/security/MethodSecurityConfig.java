package com.ecommerce.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.function.Supplier;

/**
 * Global method security configuration for all microservices.
 * Enables @PreAuthorize, @PostAuthorize, @Secured annotations.
 */
@Configuration
@EnableMethodSecurity(
    prePostEnabled = true,  // Enables @PreAuthorize and @PostAuthorize
    securedEnabled = true,  // Enables @Secured
    jsr250Enabled = true    // Enables @RolesAllowed
)
public class MethodSecurityConfig {

    /**
     * Custom authorization manager for role-based access control.
     * Can be extended for more complex authorization logic.
     */
    @Bean
    public AuthorizationManager<Object> customAuthorizationManager() {
        return (authentication, object) -> {
            Supplier<Authentication> authSupplier = authentication;
            Authentication auth = authSupplier.get();

            if (auth == null || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }

            return new AuthorizationDecision(true);
        };
    }
}
