package com.ecommerce.monitoring.controller;

import com.ecommerce.monitoring.service.AppDynamicsIntegrationService;
import com.ecommerce.monitoring.service.AppDynamicsAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for AppDynamics integration management and monitoring
 */
@RestController
@RequestMapping("/api/monitoring/appdynamics")
public class AppDynamicsController {
    
    private static final Logger logger = LoggerFactory.getLogger(AppDynamicsController.class);
    
    private final AppDynamicsIntegrationService appDynamicsService;
    private final AppDynamicsAuthService authService;
    
    public AppDynamicsController(AppDynamicsIntegrationService appDynamicsService,
                               AppDynamicsAuthService authService) {
        this.appDynamicsService = appDynamicsService;
        this.authService = authService;
    }
    
    /**
     * Get AppDynamics integration health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            boolean isConfigured = authService.isConfigured();
            boolean isHealthy = appDynamicsService.isHealthy();
            AppDynamicsAuthService.TokenInfo tokenInfo = authService.getTokenInfo();
            
            health.put("configured", isConfigured);
            health.put("healthy", isHealthy);
            health.put("status", isHealthy ? "UP" : "DOWN");
            
            // Token information
            Map<String, Object> tokenStatus = new HashMap<>();
            tokenStatus.put("hasToken", tokenInfo.hasToken());
            tokenStatus.put("tokenType", tokenInfo.getTokenType());
            tokenStatus.put("isExpired", tokenInfo.isExpired());
            
            if (tokenInfo.hasToken()) {
                tokenStatus.put("expiresAt", tokenInfo.getExpiryTime());
                tokenStatus.put("secondsUntilExpiry", tokenInfo.getSecondsUntilExpiry());
            }
            
            health.put("authentication", tokenStatus);
            
            // Additional diagnostics
            Map<String, Object> diagnostics = new HashMap<>();
            diagnostics.put("lastHealthCheck", java.time.LocalDateTime.now());
            
            if (!isConfigured) {
                diagnostics.put("configurationIssue", "Missing OAuth2 client ID or secret");
            }
            
            if (isConfigured && !isHealthy) {
                diagnostics.put("connectivityIssue", "Unable to connect to AppDynamics API");
            }
            
            health.put("diagnostics", diagnostics);
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error checking AppDynamics health", e);
            
            health.put("configured", false);
            health.put("healthy", false);
            health.put("status", "ERROR");
            health.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(health);
        }
    }
    
    /**
     * Get detailed token information
     */
    @GetMapping("/token/info")
    public ResponseEntity<Map<String, Object>> getTokenInfo() {
        try {
            AppDynamicsAuthService.TokenInfo tokenInfo = authService.getTokenInfo();
            
            Map<String, Object> info = new HashMap<>();
            info.put("hasToken", tokenInfo.hasToken());
            info.put("tokenType", tokenInfo.getTokenType());
            info.put("isExpired", tokenInfo.isExpired());
            
            if (tokenInfo.hasToken()) {
                info.put("expiresAt", tokenInfo.getExpiryTime());
                info.put("secondsUntilExpiry", tokenInfo.getSecondsUntilExpiry());
                info.put("minutesUntilExpiry", tokenInfo.getSecondsUntilExpiry() / 60);
            }
            
            info.put("configured", authService.isConfigured());
            info.put("lastChecked", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            logger.error("Error getting token info", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Force token refresh
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken() {
        try {
            logger.info("Manual token refresh requested");
            
            // Get token info before refresh
            AppDynamicsAuthService.TokenInfo beforeInfo = authService.getTokenInfo();
            
            // Force refresh
            authService.refreshToken();
            
            // Get token info after refresh
            AppDynamicsAuthService.TokenInfo afterInfo = authService.getTokenInfo();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Token refreshed successfully");
            result.put("refreshedAt", java.time.LocalDateTime.now());
            
            Map<String, Object> tokenDetails = new HashMap<>();
            tokenDetails.put("hasToken", afterInfo.hasToken());
            tokenDetails.put("expiresAt", afterInfo.getExpiryTime());
            tokenDetails.put("secondsUntilExpiry", afterInfo.getSecondsUntilExpiry());
            
            result.put("newToken", tokenDetails);
            
            logger.info("Token refresh completed successfully");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error refreshing token", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Token refresh failed");
            error.put("error", e.getMessage());
            error.put("failedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Validate current token
     */
    @PostMapping("/token/validate")
    public ResponseEntity<Map<String, Object>> validateToken() {
        try {
            boolean isValid = authService.validateToken();
            
            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("validatedAt", java.time.LocalDateTime.now());
            
            if (isValid) {
                AppDynamicsAuthService.TokenInfo tokenInfo = authService.getTokenInfo();
                result.put("expiresAt", tokenInfo.getExpiryTime());
                result.put("secondsUntilExpiry", tokenInfo.getSecondsUntilExpiry());
            } else {
                result.put("message", "Token validation failed");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error validating token", e);
            return ResponseEntity.status(500).body(Map.of(
                "valid", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Revoke current token
     */
    @PostMapping("/token/revoke")
    public ResponseEntity<Map<String, Object>> revokeToken() {
        try {
            logger.info("Manual token revocation requested");
            
            authService.revokeToken();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Token revoked successfully");
            result.put("revokedAt", java.time.LocalDateTime.now());
            
            logger.info("Token revocation completed successfully");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error revoking token", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Token revocation failed");
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Test AppDynamics API connectivity
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            logger.info("Testing AppDynamics API connectivity");
            
            long startTime = System.currentTimeMillis();
            boolean isHealthy = appDynamicsService.isHealthy();
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> result = new HashMap<>();
            result.put("connected", isHealthy);
            result.put("responseTime", duration);
            result.put("testedAt", java.time.LocalDateTime.now());
            
            if (isHealthy) {
                result.put("message", "Successfully connected to AppDynamics API");
                result.put("status", "CONNECTED");
            } else {
                result.put("message", "Failed to connect to AppDynamics API");
                result.put("status", "DISCONNECTED");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("connected", false);
            error.put("status", "ERROR");
            error.put("message", "Connection test failed");
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get configuration status
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        try {
            config.put("configured", authService.isConfigured());
            config.put("oauth2Enabled", true);
            
            // Don't expose sensitive data, just indicate what's configured
            Map<String, Object> configDetails = new HashMap<>();
            configDetails.put("hasClientId", authService.isConfigured()); // This checks both ID and secret
            configDetails.put("hasClientSecret", authService.isConfigured());
            
            config.put("authenticationMethod", "OAuth2");
            config.put("configurationDetails", configDetails);
            config.put("checkedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            logger.error("Error getting configuration", e);
            config.put("error", e.getMessage());
            return ResponseEntity.status(500).body(config);
        }
    }
}