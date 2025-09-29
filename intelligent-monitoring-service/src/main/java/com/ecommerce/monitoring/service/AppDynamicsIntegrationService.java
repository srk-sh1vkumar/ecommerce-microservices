package com.ecommerce.monitoring.service;

import com.ecommerce.monitoring.entity.MonitoringEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AppDynamicsIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AppDynamicsIntegrationService.class);
    
    @Value("${appdynamics.controller.host:}")
    private String controllerHost;
    
    @Value("${appdynamics.controller.port:443}")
    private String controllerPort;
    
    @Value("${appdynamics.application.name:ecommerce-microservices}")
    private String applicationName;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MonitoringEventService monitoringEventService;
    private final AppDynamicsAuthService authService;
    
    public AppDynamicsIntegrationService(MonitoringEventService monitoringEventService, 
                                       AppDynamicsAuthService authService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.monitoringEventService = monitoringEventService;
        this.authService = authService;
    }
    
    /**
     * Fetch business transactions from AppDynamics
     */
    public List<MonitoringEvent> fetchBusinessTransactions() {
        List<MonitoringEvent> events = new ArrayList<>();
        
        try {
            String url = buildApiUrl("/controller/rest/applications/" + applicationName + "/business-transactions");
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode transactions = objectMapper.readTree(response.getBody());
                
                for (JsonNode transaction : transactions) {
                    MonitoringEvent event = createEventFromTransaction(transaction);
                    events.add(event);
                }
                
                logger.info("Fetched {} business transactions from AppDynamics", events.size());
            }
            
        } catch (Exception e) {
            logger.error("Error fetching business transactions from AppDynamics", e);
        }
        
        return events;
    }
    
    /**
     * Fetch errors and exceptions from AppDynamics
     */
    public List<MonitoringEvent> fetchErrors(LocalDateTime since) {
        List<MonitoringEvent> events = new ArrayList<>();
        
        try {
            String timeRange = formatTimeRange(since);
            String url = buildApiUrl("/controller/rest/applications/" + applicationName + "/problems/healthrule-violations") + 
                        "?time-range-type=BEFORE_NOW&duration-in-mins=60";
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode violations = objectMapper.readTree(response.getBody());
                
                for (JsonNode violation : violations) {
                    MonitoringEvent event = createEventFromViolation(violation);
                    events.add(event);
                }
                
                logger.info("Fetched {} error events from AppDynamics", events.size());
            }
            
        } catch (Exception e) {
            logger.error("Error fetching errors from AppDynamics", e);
        }
        
        return events;
    }
    
    /**
     * Fetch performance metrics from AppDynamics
     */
    public List<MonitoringEvent> fetchPerformanceMetrics() {
        List<MonitoringEvent> events = new ArrayList<>();
        
        try {
            String url = buildApiUrl("/controller/rest/applications/" + applicationName + "/metric-data") +
                        "?metric-path=Application Infrastructure Performance|*|*&time-range-type=BEFORE_NOW&duration-in-mins=15";
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode metrics = objectMapper.readTree(response.getBody());
                
                for (JsonNode metric : metrics) {
                    MonitoringEvent event = createEventFromMetric(metric);
                    if (event != null) {
                        events.add(event);
                    }
                }
                
                logger.info("Fetched {} performance metrics from AppDynamics", events.size());
            }
            
        } catch (Exception e) {
            logger.error("Error fetching performance metrics from AppDynamics", e);
        }
        
        return events;
    }
    
    /**
     * Fetch error snapshots with detailed stack traces
     */
    public List<MonitoringEvent> fetchErrorSnapshots(String businessTransactionId) {
        List<MonitoringEvent> events = new ArrayList<>();
        
        try {
            String url = buildApiUrl("/controller/rest/applications/" + applicationName + 
                        "/business-transactions/" + businessTransactionId + "/snapshots") +
                        "?time-range-type=BEFORE_NOW&duration-in-mins=60&severity=ERROR";
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode snapshots = objectMapper.readTree(response.getBody());
                
                for (JsonNode snapshot : snapshots) {
                    MonitoringEvent event = createEventFromSnapshot(snapshot);
                    events.add(event);
                }
                
                logger.info("Fetched {} error snapshots from AppDynamics", events.size());
            }
            
        } catch (Exception e) {
            logger.error("Error fetching error snapshots from AppDynamics", e);
        }
        
        return events;
    }
    
    /**
     * Send correlation data back to AppDynamics
     */
    public void sendCorrelationData(String traceId, Map<String, Object> correlationData) {
        try {
            // Custom event creation for correlation tracking
            Map<String, Object> customEvent = new HashMap<>();
            customEvent.put("eventType", "Intelligent_Monitoring_Correlation");
            customEvent.put("traceId", traceId);
            customEvent.put("correlationData", correlationData);
            customEvent.put("timestamp", System.currentTimeMillis());
            
            String url = buildApiUrl("/controller/rest/applications/" + applicationName + "/events");
            
            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String requestBody = objectMapper.writeValueAsString(customEvent);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Successfully sent correlation data to AppDynamics for trace: {}", traceId);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to send correlation data to AppDynamics", e);
        }
    }
    
    // Private helper methods
    
    private String buildApiUrl(String endpoint) {
        return String.format("https://%s:%s%s", controllerHost, controllerPort, endpoint);
    }
    
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        
        // Use OAuth2 Bearer token authentication
        String authHeader = authService.getAuthorizationHeader();
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        } else {
            logger.warn("No valid OAuth2 token available for AppDynamics API calls");
            throw new RuntimeException("AppDynamics authentication failed - no valid token");
        }
        
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.set("Content-Type", "application/json");
        
        return headers;
    }
    
    private MonitoringEvent createEventFromTransaction(JsonNode transaction) {
        MonitoringEvent event = new MonitoringEvent("appdynamics", "performance", "info", extractServiceName(transaction));
        
        event.setDescription("Business Transaction Performance");
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("transactionName", transaction.path("name").asText());
        metrics.put("tierName", transaction.path("tierName").asText());
        metrics.put("calls", transaction.path("numberOfCalls").asLong());
        metrics.put("averageResponseTime", transaction.path("averageResponseTime").asDouble());
        metrics.put("errorsPerMinute", transaction.path("errorsPerMinute").asDouble());
        
        event.setMetrics(metrics);
        
        return event;
    }
    
    private MonitoringEvent createEventFromViolation(JsonNode violation) {
        MonitoringEvent event = new MonitoringEvent("appdynamics", "error", 
                                                   mapSeverity(violation.path("severity").asText()), 
                                                   extractServiceFromViolation(violation));
        
        event.setDescription(violation.path("name").asText());
        event.setErrorSignature(generateErrorSignature(violation));
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("violationId", violation.path("id").asLong());
        metrics.put("startTime", violation.path("startTimeInMillis").asLong());
        metrics.put("endTime", violation.path("endTimeInMillis").asLong());
        
        event.setMetrics(metrics);
        
        return event;
    }
    
    private MonitoringEvent createEventFromMetric(JsonNode metric) {
        String metricName = metric.path("metricName").asText();
        
        // Only process relevant performance metrics
        if (!isRelevantMetric(metricName)) {
            return null;
        }
        
        MonitoringEvent event = new MonitoringEvent("appdynamics", "performance", "info", extractServiceFromMetric(metric));
        
        event.setDescription("Performance Metric: " + metricName);
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("metricName", metricName);
        metrics.put("value", metric.path("metricValue").asDouble());
        metrics.put("metricPath", metric.path("metricPath").asText());
        
        event.setMetrics(metrics);
        
        return event;
    }
    
    private MonitoringEvent createEventFromSnapshot(JsonNode snapshot) {
        MonitoringEvent event = new MonitoringEvent("appdynamics", "error", "high", extractServiceFromSnapshot(snapshot));
        
        event.setDescription("Error Snapshot");
        event.setErrorSignature(generateSnapshotSignature(snapshot));
        event.setStackTrace(extractStackTrace(snapshot));
        event.setCodeLocation(extractCodeLocation(snapshot));
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("snapshotId", snapshot.path("requestGUID").asText());
        metrics.put("duration", snapshot.path("timeTakenInMillis").asLong());
        metrics.put("businessTransactionId", snapshot.path("businessTransactionId").asLong());
        
        event.setMetrics(metrics);
        
        return event;
    }
    
    private String extractServiceName(JsonNode transaction) {
        String tierName = transaction.path("tierName").asText();
        if (tierName != null && !tierName.isEmpty()) {
            return tierName.toLowerCase().replace("-", "");
        }
        return "unknown-service";
    }
    
    private String extractServiceFromViolation(JsonNode violation) {
        String entityName = violation.path("affectedEntityDefinition").path("entityName").asText();
        if (entityName != null && !entityName.isEmpty()) {
            return entityName.toLowerCase().replace("-", "");
        }
        return "unknown-service";
    }
    
    private String extractServiceFromMetric(JsonNode metric) {
        String metricPath = metric.path("metricPath").asText();
        String[] pathParts = metricPath.split("\\|");
        if (pathParts.length >= 2) {
            return pathParts[1].toLowerCase().replace("-", "");
        }
        return "unknown-service";
    }
    
    private String extractServiceFromSnapshot(JsonNode snapshot) {
        String tierName = snapshot.path("applicationComponentName").asText();
        if (tierName != null && !tierName.isEmpty()) {
            return tierName.toLowerCase().replace("-", "");
        }
        return "unknown-service";
    }
    
    private String mapSeverity(String appDynamicsSeverity) {
        switch (appDynamicsSeverity.toLowerCase()) {
            case "critical": return "critical";
            case "warning": return "medium";
            case "info": return "low";
            default: return "medium";
        }
    }
    
    private String generateErrorSignature(JsonNode violation) {
        String name = violation.path("name").asText();
        String entity = violation.path("affectedEntityDefinition").path("entityName").asText();
        return String.format("%s_%s", name, entity).replaceAll("\\s+", "_");
    }
    
    private String generateSnapshotSignature(JsonNode snapshot) {
        String transactionName = snapshot.path("businessTransactionName").asText();
        String errorMessage = extractErrorMessage(snapshot);
        return String.format("%s_%s", transactionName, errorMessage).replaceAll("\\s+", "_");
    }
    
    private String extractStackTrace(JsonNode snapshot) {
        // Extract stack trace from snapshot call graph
        JsonNode callGraph = snapshot.path("callGraph");
        StringBuilder stackTrace = new StringBuilder();
        
        extractStackTraceRecursive(callGraph, stackTrace, 0);
        
        return stackTrace.toString();
    }
    
    private void extractStackTraceRecursive(JsonNode node, StringBuilder stackTrace, int depth) {
        if (node.has("className") && node.has("methodName")) {
            String indent = "  ".repeat(depth);
            stackTrace.append(indent)
                     .append(node.path("className").asText())
                     .append(".")
                     .append(node.path("methodName").asText())
                     .append("\n");
        }
        
        JsonNode children = node.path("children");
        if (children.isArray()) {
            for (JsonNode child : children) {
                extractStackTraceRecursive(child, stackTrace, depth + 1);
            }
        }
    }
    
    private String extractCodeLocation(JsonNode snapshot) {
        JsonNode callGraph = snapshot.path("callGraph");
        if (callGraph.has("className") && callGraph.has("methodName")) {
            return callGraph.path("className").asText() + "." + callGraph.path("methodName").asText();
        }
        return null;
    }
    
    private String extractErrorMessage(JsonNode snapshot) {
        JsonNode errors = snapshot.path("errorDetails");
        if (errors.isArray() && errors.size() > 0) {
            return errors.get(0).path("message").asText();
        }
        return "unknown_error";
    }
    
    private boolean isRelevantMetric(String metricName) {
        return metricName.contains("Response Time") || 
               metricName.contains("Calls per Minute") ||
               metricName.contains("Errors per Minute") ||
               metricName.contains("CPU") ||
               metricName.contains("Memory");
    }
    
    private String formatTimeRange(LocalDateTime since) {
        return since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    /**
     * Check if AppDynamics services are healthy
     */
    public boolean isHealthy() {
        try {
            // Check if OAuth2 authentication is properly configured
            if (!authService.isConfigured()) {
                logger.warn("AppDynamics OAuth2 not configured");
                return false;
            }
            
            // Validate current token
            if (!authService.validateToken()) {
                logger.warn("AppDynamics token validation failed");
                return false;
            }
            
            // Test API connectivity with a simple applications call
            String url = buildApiUrl("/controller/rest/applications");
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            
            if (isHealthy) {
                logger.debug("AppDynamics health check passed");
            } else {
                logger.warn("AppDynamics health check failed with status: {}", response.getStatusCode());
            }
            
            return isHealthy;
            
        } catch (Exception e) {
            logger.warn("AppDynamics health check failed", e);
            return false;
        }
    }
    
    /**
     * Get OAuth2 token information for monitoring dashboard
     */
    public AppDynamicsAuthService.TokenInfo getTokenInfo() {
        return authService.getTokenInfo();
    }
    
    /**
     * Force token refresh (for administrative purposes)
     */
    public void refreshToken() {
        authService.refreshToken();
    }
}