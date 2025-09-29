package com.ecommerce.monitoring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.locks.ReentrantLock;

/**
 * OAuth2 Authentication Service for AppDynamics API
 * Manages token lifecycle and automatic refresh
 */
@Service
public class AppDynamicsAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AppDynamicsAuthService.class);
    
    @Value("${appdynamics.oauth2.client-id:}")
    private String clientId;
    
    @Value("${appdynamics.oauth2.client-secret:}")
    private String clientSecret;
    
    @Value("${appdynamics.controller.host:}")
    private String controllerHost;
    
    @Value("${appdynamics.controller.port:443}")
    private String controllerPort;
    
    @Value("${appdynamics.oauth2.scope:read}")
    private String scope;
    
    @Value("${appdynamics.oauth2.token-buffer-minutes:5}")
    private int tokenBufferMinutes;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ReentrantLock tokenLock = new ReentrantLock();
    
    // Token storage
    private volatile String accessToken;
    private volatile LocalDateTime tokenExpiryTime;
    private volatile String tokenType = "Bearer";
    
    public AppDynamicsAuthService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get valid access token, refreshing if necessary
     */
    public String getAccessToken() {
        tokenLock.lock();
        try {
            if (isTokenExpired() || accessToken == null) {
                refreshToken();
            }
            return accessToken;
        } finally {
            tokenLock.unlock();
        }
    }
    
    /**
     * Get authorization header with Bearer token
     */
    public String getAuthorizationHeader() {
        String token = getAccessToken();
        if (token != null) {
            return tokenType + " " + token;
        }
        return null;
    }
    
    /**
     * Check if authentication is properly configured
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.trim().isEmpty() &&
               clientSecret != null && !clientSecret.trim().isEmpty() &&
               controllerHost != null && !controllerHost.trim().isEmpty();
    }
    
    /**
     * Validate current token by making a test API call
     */
    public boolean validateToken() {
        if (accessToken == null) {
            return false;
        }
        
        try {
            String testUrl = buildApiUrl("/controller/rest/applications");
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", getAuthorizationHeader());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                testUrl, HttpMethod.GET, entity, String.class);
            
            boolean isValid = response.getStatusCode().is2xxSuccessful();
            
            if (!isValid) {
                logger.warn("Token validation failed with status: {}", response.getStatusCode());
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.warn("Token validation failed", e);
            return false;
        }
    }
    
    /**
     * Force token refresh
     */
    public void refreshToken() {
        if (!isConfigured()) {
            logger.error("AppDynamics OAuth2 not properly configured. Missing client ID, secret, or controller host.");
            return;
        }
        
        try {
            logger.debug("Refreshing AppDynamics access token...");
            
            String tokenUrl = buildTokenUrl();
            
            HttpHeaders headers = createTokenRequestHeaders();
            MultiValueMap<String, String> body = createTokenRequestBody();
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                processTokenResponse(response.getBody());
                logger.info("Successfully refreshed AppDynamics access token");
            } else {
                logger.error("Failed to refresh token. Status: {}, Body: {}", 
                           response.getStatusCode(), response.getBody());
                handleTokenError(response);
            }
            
        } catch (Exception e) {
            logger.error("Error refreshing AppDynamics access token", e);
            accessToken = null;
            tokenExpiryTime = null;
        }
    }
    
    /**
     * Revoke current token
     */
    public void revokeToken() {
        if (accessToken == null) {
            return;
        }
        
        try {
            String revokeUrl = buildRevokeUrl();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", createBasicAuthHeader());
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", accessToken);
            body.add("token_type_hint", "access_token");
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                revokeUrl, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully revoked AppDynamics access token");
            } else {
                logger.warn("Failed to revoke token. Status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.warn("Error revoking AppDynamics access token", e);
        } finally {
            // Clear local token regardless of revocation success
            accessToken = null;
            tokenExpiryTime = null;
        }
    }
    
    /**
     * Get token information for monitoring/debugging
     */
    public TokenInfo getTokenInfo() {
        return new TokenInfo(
            accessToken != null,
            tokenExpiryTime,
            isTokenExpired(),
            tokenType
        );
    }
    
    // Private helper methods
    
    private boolean isTokenExpired() {
        if (tokenExpiryTime == null) {
            return true;
        }
        
        // Consider token expired if it expires within the buffer time
        LocalDateTime bufferTime = LocalDateTime.now().plus(tokenBufferMinutes, ChronoUnit.MINUTES);
        return tokenExpiryTime.isBefore(bufferTime);
    }
    
    private String buildTokenUrl() {
        return String.format("https://%s:%s/controller/api/oauth/access_token", 
                           controllerHost, controllerPort);
    }
    
    private String buildRevokeUrl() {
        return String.format("https://%s:%s/controller/api/oauth/revoke_token", 
                           controllerHost, controllerPort);
    }
    
    private String buildApiUrl(String endpoint) {
        return String.format("https://%s:%s%s", controllerHost, controllerPort, endpoint);
    }
    
    private HttpHeaders createTokenRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", createBasicAuthHeader());
        return headers;
    }
    
    private String createBasicAuthHeader() {
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + encodedCredentials;
    }
    
    private MultiValueMap<String, String> createTokenRequestBody() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", scope);
        return body;
    }
    
    private void processTokenResponse(String responseBody) {
        try {
            JsonNode tokenResponse = objectMapper.readTree(responseBody);
            
            accessToken = tokenResponse.path("access_token").asText();
            String tokenTypeFromResponse = tokenResponse.path("token_type").asText("Bearer");
            int expiresIn = tokenResponse.path("expires_in").asInt(3600); // Default 1 hour
            
            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("No access token in response");
            }
            
            tokenType = tokenTypeFromResponse;
            tokenExpiryTime = LocalDateTime.now().plus(expiresIn, ChronoUnit.SECONDS);
            
            logger.debug("Token expires at: {}", tokenExpiryTime);
            
            // Log additional token info if present
            if (tokenResponse.has("scope")) {
                logger.debug("Token scope: {}", tokenResponse.path("scope").asText());
            }
            
        } catch (Exception e) {
            logger.error("Error processing token response: {}", responseBody, e);
            throw new RuntimeException("Failed to process token response", e);
        }
    }
    
    private void handleTokenError(ResponseEntity<String> response) {
        try {
            if (response.getBody() != null) {
                JsonNode errorResponse = objectMapper.readTree(response.getBody());
                String error = errorResponse.path("error").asText("unknown_error");
                String errorDescription = errorResponse.path("error_description").asText("");
                
                logger.error("OAuth2 Error: {} - {}", error, errorDescription);
                
                // Handle specific error cases
                switch (error) {
                    case "invalid_client":
                        logger.error("Invalid client credentials. Check client ID and secret.");
                        break;
                    case "invalid_scope":
                        logger.error("Invalid scope requested: {}", scope);
                        break;
                    case "server_error":
                        logger.error("AppDynamics server error. Controller may be down.");
                        break;
                    default:
                        logger.error("Unhandled OAuth2 error: {}", error);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing token error response", e);
        }
    }
    
    /**
     * Token information class for monitoring
     */
    public static class TokenInfo {
        private final boolean hasToken;
        private final LocalDateTime expiryTime;
        private final boolean isExpired;
        private final String tokenType;
        
        public TokenInfo(boolean hasToken, LocalDateTime expiryTime, boolean isExpired, String tokenType) {
            this.hasToken = hasToken;
            this.expiryTime = expiryTime;
            this.isExpired = isExpired;
            this.tokenType = tokenType;
        }
        
        public boolean hasToken() { return hasToken; }
        public LocalDateTime getExpiryTime() { return expiryTime; }
        public boolean isExpired() { return isExpired; }
        public String getTokenType() { return tokenType; }
        
        public long getSecondsUntilExpiry() {
            if (expiryTime == null) return 0;
            return ChronoUnit.SECONDS.between(LocalDateTime.now(), expiryTime);
        }
    }
}