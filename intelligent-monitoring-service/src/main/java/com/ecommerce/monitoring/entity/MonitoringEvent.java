package com.ecommerce.monitoring.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "intelligent_monitoring_events")
public class MonitoringEvent {
    
    @Id
    private String id;
    
    @Indexed
    private String source; // "appdynamics", "opentelemetry", "frontend", "loadtest"
    
    @Indexed
    private String eventType; // "error", "performance", "business", "trace"
    
    @Indexed
    private String severity; // "critical", "high", "medium", "low", "info"
    
    @Indexed
    private String serviceName;
    
    @Indexed
    private String correlationId;
    
    private String traceId;
    private String spanId;
    
    private String errorSignature;
    private String stackTrace;
    private String codeLocation;
    
    private Map<String, Object> metrics;
    private Map<String, String> tags;
    private Map<String, Object> businessContext;
    
    @CreatedDate
    @Indexed(expireAfterSeconds = 2592000) // 30 days TTL
    private LocalDateTime timestamp;
    
    private String description;
    private String resolution;
    private boolean autoFixed;
    private String fixCommitId;
    
    // Constructors
    public MonitoringEvent() {}
    
    public MonitoringEvent(String source, String eventType, String severity, String serviceName) {
        this.source = source;
        this.eventType = eventType;
        this.severity = severity;
        this.serviceName = serviceName;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    
    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }
    
    public String getErrorSignature() { return errorSignature; }
    public void setErrorSignature(String errorSignature) { this.errorSignature = errorSignature; }
    
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    
    public String getCodeLocation() { return codeLocation; }
    public void setCodeLocation(String codeLocation) { this.codeLocation = codeLocation; }
    
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    
    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) { this.tags = tags; }
    
    public Map<String, Object> getBusinessContext() { return businessContext; }
    public void setBusinessContext(Map<String, Object> businessContext) { this.businessContext = businessContext; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    
    public boolean isAutoFixed() { return autoFixed; }
    public void setAutoFixed(boolean autoFixed) { this.autoFixed = autoFixed; }
    
    public String getFixCommitId() { return fixCommitId; }
    public void setFixCommitId(String fixCommitId) { this.fixCommitId = fixCommitId; }
}