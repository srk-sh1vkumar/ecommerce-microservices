package com.ecommerce.monitoring.service;

import com.ecommerce.monitoring.entity.AuditEvent;
import com.ecommerce.monitoring.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for audit logging and compliance tracking
 */
@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;
    
    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Log an audit event
     */
    public void logEvent(String eventType, Map<String, Object> eventData) {
        logEvent(eventType, eventData, null, null);
    }
    
    /**
     * Log an audit event with user context
     */
    public void logEvent(String eventType, Map<String, Object> eventData, String userId, String sessionId) {
        try {
            AuditEvent auditEvent = new AuditEvent();
            auditEvent.setEventId(generateEventId());
            auditEvent.setEventType(eventType);
            auditEvent.setUserId(userId);
            auditEvent.setSessionId(sessionId);
            auditEvent.setTimestamp(LocalDateTime.now());
            auditEvent.setEventData(objectMapper.writeValueAsString(eventData));
            auditEvent.setSource("intelligent-monitoring-service");
            
            // Extract severity if present
            if (eventData.containsKey("severity")) {
                auditEvent.setSeverity(eventData.get("severity").toString());
            } else {
                auditEvent.setSeverity("INFO");
            }
            
            // Extract category if present
            if (eventData.containsKey("category")) {
                auditEvent.setCategory(eventData.get("category").toString());
            } else {
                auditEvent.setCategory(determineCategory(eventType));
            }
            
            auditEventRepository.save(auditEvent);
            
            logger.debug("Audit event logged: {} (ID: {})", eventType, auditEvent.getEventId());
            
        } catch (Exception e) {
            logger.error("Failed to log audit event: {}", eventType, e);
        }
    }
    
    /**
     * Log security event
     */
    public void logSecurityEvent(String eventType, String userId, String details, String severity) {
        Map<String, Object> eventData = Map.of(
            "details", details,
            "severity", severity,
            "category", "SECURITY",
            "timestamp", LocalDateTime.now()
        );
        
        logEvent(eventType, eventData, userId, null);
        
        // Also log to security log
        logger.warn("SECURITY EVENT [{}]: User={}, Details={}", eventType, userId, details);
    }
    
    /**
     * Log code fix audit event
     */
    public void logCodeFixEvent(String reviewId, String action, String userId, Map<String, Object> details) {
        Map<String, Object> eventData = Map.of(
            "reviewId", reviewId,
            "action", action,
            "details", details,
            "category", "CODE_FIX",
            "severity", "INFO"
        );
        
        logEvent("code_fix_" + action.toLowerCase(), eventData, userId, null);
    }
    
    /**
     * Log monitoring event
     */
    public void logMonitoringEvent(String service, String eventType, Map<String, Object> metrics) {
        Map<String, Object> eventData = Map.of(
            "service", service,
            "metrics", metrics,
            "category", "MONITORING",
            "severity", determineSeverityFromMetrics(metrics)
        );
        
        logEvent(eventType, eventData);
    }
    
    /**
     * Log system access event
     */
    public void logAccessEvent(String userId, String resource, String action, boolean success) {
        Map<String, Object> eventData = Map.of(
            "resource", resource,
            "action", action,
            "success", success,
            "category", "ACCESS",
            "severity", success ? "INFO" : "WARN"
        );
        
        logEvent("access_" + (success ? "granted" : "denied"), eventData, userId, null);
    }
    
    /**
     * Get audit events by type
     */
    public List<AuditEvent> getEventsByType(String eventType, int limit) {
        return auditEventRepository.findByEventTypeOrderByTimestampDesc(eventType)
                .stream()
                .limit(limit)
                .toList();
    }
    
    /**
     * Get audit events by user
     */
    public List<AuditEvent> getEventsByUser(String userId, int limit) {
        return auditEventRepository.findByUserIdOrderByTimestampDesc(userId)
                .stream()
                .limit(limit)
                .toList();
    }
    
    /**
     * Get audit events by date range
     */
    public List<AuditEvent> getEventsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditEventRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate);
    }
    
    /**
     * Get security audit events
     */
    public List<AuditEvent> getSecurityEvents(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return auditEventRepository.findByCategoryAndTimestampAfterOrderByTimestampDesc("SECURITY", since);
    }
    
    /**
     * Get code fix audit events
     */
    public List<AuditEvent> getCodeFixEvents(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return auditEventRepository.findByCategoryAndTimestampAfterOrderByTimestampDesc("CODE_FIX", since);
    }
    
    /**
     * Count events by type in time period
     */
    public long countEventsByType(String eventType, LocalDateTime since) {
        return auditEventRepository.countByEventTypeAndTimestampAfter(eventType, since);
    }
    
    /**
     * Get failed access attempts
     */
    public List<AuditEvent> getFailedAccessAttempts(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return auditEventRepository.findByEventTypeContainingAndTimestampAfterOrderByTimestampDesc(
            "access_denied", since);
    }
    
    private String generateEventId() {
        return "AUDIT_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private String determineCategory(String eventType) {
        if (eventType.contains("security") || eventType.contains("auth") || eventType.contains("access")) {
            return "SECURITY";
        } else if (eventType.contains("code") || eventType.contains("fix") || eventType.contains("review")) {
            return "CODE_FIX";
        } else if (eventType.contains("monitor") || eventType.contains("metric")) {
            return "MONITORING";
        } else if (eventType.contains("system") || eventType.contains("service")) {
            return "SYSTEM";
        } else {
            return "GENERAL";
        }
    }
    
    private String determineSeverityFromMetrics(Map<String, Object> metrics) {
        // Simple logic to determine severity from metrics
        if (metrics.containsKey("errorRate")) {
            Double errorRate = (Double) metrics.get("errorRate");
            if (errorRate != null && errorRate > 10.0) {
                return "CRITICAL";
            } else if (errorRate != null && errorRate > 5.0) {
                return "WARN";
            }
        }
        
        if (metrics.containsKey("responseTime")) {
            Integer responseTime = (Integer) metrics.get("responseTime");
            if (responseTime != null && responseTime > 5000) {
                return "WARN";
            }
        }
        
        return "INFO";
    }
}