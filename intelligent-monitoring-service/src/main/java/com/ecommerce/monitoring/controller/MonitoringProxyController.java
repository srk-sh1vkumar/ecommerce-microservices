package com.ecommerce.monitoring.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified monitoring proxy controller to provide single-endpoint access to all monitoring services
 * Handles authentication, request routing, and response transformation
 */
@RestController
@RequestMapping("/api/monitoring/proxy")
@CrossOrigin(origins = {"https://admin.ecommerce.com", "http://localhost:4200"})
public class MonitoringProxyController {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringProxyController.class);
    
    @Value("${monitoring.services.grafana.url:http://localhost:3000}")
    private String grafanaUrl;
    
    @Value("${monitoring.services.prometheus.url:http://localhost:9090}")
    private String prometheusUrl;
    
    @Value("${monitoring.services.alertmanager.url:http://localhost:9093}")
    private String alertmanagerUrl;
    
    @Value("${monitoring.services.tempo.url:http://localhost:3200}")
    private String tempoUrl;
    
    @Value("${monitoring.services.elasticsearch.url:http://localhost:5601}")
    private String elasticsearchUrl;
    
    @Value("${monitoring.proxy.enabled:true}")
    private boolean proxyEnabled;
    
    @Value("${monitoring.proxy.authentication.enabled:false}")
    private boolean authenticationEnabled;
    
    private final RestTemplate restTemplate;
    private final Map<String, String> serviceUrls = new ConcurrentHashMap<>();
    
    public MonitoringProxyController() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    private void initializeServiceUrls() {
        // Initialize service URL mappings after @Value injection
        serviceUrls.put("grafana", grafanaUrl);
        serviceUrls.put("prometheus", prometheusUrl);
        serviceUrls.put("alertmanager", alertmanagerUrl);
        serviceUrls.put("tempo", tempoUrl);
        serviceUrls.put("elasticsearch", elasticsearchUrl);
    }
    
    /**
     * Generic proxy endpoint for all monitoring services
     */
    @RequestMapping(value = "/{service}/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyRequest(
            @PathVariable String service,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody(required = false) Object body) {
        
        if (!proxyEnabled) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Monitoring proxy is disabled",
                "service", service
            ));
        }
        
        String targetUrl = serviceUrls.get(service.toLowerCase());
        if (targetUrl == null) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "Unknown monitoring service",
                "service", service,
                "availableServices", serviceUrls.keySet()
            ));
        }
        
        try {
            // Extract the path after the service name
            String requestPath = request.getRequestURI().replaceFirst("/api/monitoring/proxy/" + service, "");
            if (requestPath.isEmpty()) {
                requestPath = "/";
            }
            
            // Build target URL
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(targetUrl + requestPath);
            
            // Add query parameters
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                uriBuilder.query(queryString);
            }
            
            URI targetUri = uriBuilder.build().toUri();
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            copyHeaders(request, headers);
            addServiceSpecificHeaders(service, headers);
            
            // Create request entity
            HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
            
            // Make the proxied request
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            ResponseEntity<Object> responseEntity = restTemplate.exchange(
                targetUri, method, requestEntity, Object.class);
            
            // Process response headers
            HttpHeaders responseHeaders = new HttpHeaders();
            copyResponseHeaders(responseEntity.getHeaders(), responseHeaders);
            
            logger.debug("Proxied request to {}: {} {} -> {}", 
                        service, method, requestPath, responseEntity.getStatusCode());
            
            return ResponseEntity.status(responseEntity.getStatusCode())
                                .headers(responseHeaders)
                                .body(responseEntity.getBody());
            
        } catch (Exception e) {
            logger.error("Error proxying request to service {}: {}", service, e.getMessage(), e);
            
            return ResponseEntity.status(502).body(Map.of(
                "error", "Proxy request failed",
                "service", service,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Health check for specific monitoring service
     */
    @GetMapping("/{service}/health")
    public ResponseEntity<Map<String, Object>> checkServiceHealth(@PathVariable String service) {
        String targetUrl = serviceUrls.get(service.toLowerCase());
        if (targetUrl == null) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "Unknown monitoring service",
                "service", service
            ));
        }
        
        try {
            String healthEndpoint = getHealthEndpoint(service);
            URI healthUri = UriComponentsBuilder.fromHttpUrl(targetUrl + healthEndpoint).build().toUri();
            
            HttpHeaders headers = new HttpHeaders();
            addServiceSpecificHeaders(service, headers);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                healthUri, HttpMethod.GET, requestEntity, Object.class);
            
            return ResponseEntity.ok(Map.of(
                "service", service,
                "status", "healthy",
                "responseCode", response.getStatusCodeValue(),
                "responseTime", System.currentTimeMillis(),
                "endpoint", healthEndpoint
            ));
            
        } catch (Exception e) {
            logger.warn("Health check failed for service {}: {}", service, e.getMessage());
            
            return ResponseEntity.status(503).body(Map.of(
                "service", service,
                "status", "unhealthy",
                "error", e.getMessage(),
                "checkedAt", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get available monitoring services and their status
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> getAvailableServices() {
        Map<String, Object> services = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, String> entry : serviceUrls.entrySet()) {
            String serviceName = entry.getKey();
            String serviceUrl = entry.getValue();
            
            Map<String, Object> serviceInfo = Map.of(
                "name", serviceName,
                "url", serviceUrl,
                "proxyPath", "/api/monitoring/proxy/" + serviceName,
                "healthEndpoint", "/api/monitoring/proxy/" + serviceName + "/health",
                "category", getServiceCategory(serviceName)
            );
            
            services.put(serviceName, serviceInfo);
        }
        
        return ResponseEntity.ok(Map.of(
            "services", services,
            "proxyEnabled", proxyEnabled,
            "authenticationEnabled", authenticationEnabled,
            "totalServices", services.size()
        ));
    }
    
    /**
     * Update service configuration
     */
    @PostMapping("/services/{service}/configure")
    public ResponseEntity<Map<String, Object>> configureService(
            @PathVariable String service,
            @RequestBody Map<String, String> config) {
        
        if (!serviceUrls.containsKey(service.toLowerCase())) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "Unknown monitoring service",
                "service", service
            ));
        }
        
        String newUrl = config.get("url");
        if (newUrl != null && !newUrl.trim().isEmpty()) {
            serviceUrls.put(service.toLowerCase(), newUrl.trim());
            
            logger.info("Updated service {} URL to: {}", service, newUrl);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "service", service,
                "newUrl", newUrl,
                "updatedAt", System.currentTimeMillis()
            ));
        }
        
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Invalid configuration",
            "service", service,
            "requiredFields", "url"
        ));
    }
    
    // Helper methods
    
    private void copyHeaders(HttpServletRequest request, HttpHeaders headers) {
        // Copy relevant headers from the original request
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            
            // Skip certain headers that shouldn't be forwarded
            if (shouldForwardHeader(headerName)) {
                String headerValue = request.getHeader(headerName);
                headers.add(headerName, headerValue);
            }
        }
    }
    
    private boolean shouldForwardHeader(String headerName) {
        String headerLower = headerName.toLowerCase();
        
        // Don't forward these headers
        return !headerLower.equals("host") &&
               !headerLower.equals("content-length") &&
               !headerLower.equals("connection") &&
               !headerLower.equals("upgrade") &&
               !headerLower.startsWith("sec-") &&
               !headerLower.equals("origin");
    }
    
    private void copyResponseHeaders(HttpHeaders sourceHeaders, HttpHeaders targetHeaders) {
        for (Map.Entry<String, java.util.List<String>> entry : sourceHeaders.entrySet()) {
            String headerName = entry.getKey();
            
            if (shouldForwardResponseHeader(headerName)) {
                for (String headerValue : entry.getValue()) {
                    targetHeaders.add(headerName, headerValue);
                }
            }
        }
        
        // Add CORS headers for frontend access
        targetHeaders.add("Access-Control-Allow-Origin", "*");
        targetHeaders.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        targetHeaders.add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
    }
    
    private boolean shouldForwardResponseHeader(String headerName) {
        String headerLower = headerName.toLowerCase();
        
        // Don't forward these response headers
        return !headerLower.equals("server") &&
               !headerLower.equals("date") &&
               !headerLower.equals("connection") &&
               !headerLower.startsWith("access-control-");
    }
    
    private void addServiceSpecificHeaders(String service, HttpHeaders headers) {
        switch (service.toLowerCase()) {
            case "grafana":
                headers.add("Accept", "application/json");
                if (authenticationEnabled) {
                    // Add Grafana authentication headers if configured
                    headers.add("Authorization", "Bearer " + getGrafanaToken());
                }
                break;
                
            case "prometheus":
                headers.add("Accept", "application/json");
                break;
                
            case "alertmanager":
                headers.add("Accept", "application/json");
                break;
                
            case "tempo":
                headers.add("Accept", "application/json");
                break;
                
            case "elasticsearch":
                headers.add("Accept", "application/json");
                if (authenticationEnabled) {
                    // Add Elasticsearch authentication if configured
                    headers.add("Authorization", "Basic " + getElasticsearchAuth());
                }
                break;
        }
    }
    
    private String getHealthEndpoint(String service) {
        switch (service.toLowerCase()) {
            case "grafana":
                return "/api/health";
            case "prometheus":
                return "/-/healthy";
            case "alertmanager":
                return "/-/healthy";
            case "tempo":
                return "/ready";
            case "elasticsearch":
                return "/api/status";
            default:
                return "/health";
        }
    }
    
    private String getServiceCategory(String service) {
        switch (service.toLowerCase()) {
            case "grafana":
                return "visualization";
            case "prometheus":
                return "metrics";
            case "alertmanager":
                return "alerting";
            case "tempo":
                return "tracing";
            case "elasticsearch":
                return "logging";
            default:
                return "monitoring";
        }
    }
    
    private String getGrafanaToken() {
        // In a real implementation, this would retrieve the actual Grafana API token
        // from secure configuration or environment variables
        return "placeholder-grafana-token";
    }
    
    private String getElasticsearchAuth() {
        // In a real implementation, this would retrieve the actual Elasticsearch credentials
        // from secure configuration or environment variables
        return "placeholder-elasticsearch-auth";
    }
}