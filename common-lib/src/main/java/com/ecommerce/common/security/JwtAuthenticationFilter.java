package com.ecommerce.common.security;

import com.ecommerce.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT Authentication Filter that validates JWT tokens for each request.
 * Integrates with Spring Security to authenticate users based on JWT tokens.
 *
 * Features:
 * - Extracts JWT token from Authorization header
 * - Validates token signature and expiration
 * - Sets Spring Security authentication context
 * - Handles token errors gracefully
 *
 * @author E-commerce Development Team
 * @version 1.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Create authentication token with user details
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    getAuthorities()
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    logger.debug("JWT authentication successful for user: {}", email);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from the Authorization header.
     *
     * @param request HTTP request
     * @return JWT token or null if not present
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Gets default authorities for authenticated users.
     * Override this method to customize role extraction from JWT.
     *
     * @return List of authorities
     */
    protected List<SimpleGrantedAuthority> getAuthorities() {
        // Default role for authenticated users
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip JWT validation for public endpoints
        return path.contains("/login") ||
               path.contains("/register") ||
               path.contains("/actuator/health") ||
               path.contains("/actuator/info") ||
               path.contains("/swagger") ||
               path.contains("/api-docs");
    }
}
