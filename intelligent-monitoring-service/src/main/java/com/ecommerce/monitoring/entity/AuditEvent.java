package com.ecommerce.monitoring.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Entity representing an audit event for compliance and security tracking
 */
@Document(collection = "audit_events")
public class AuditEvent {
    
    @Id
    private String id;
    
    @Indexed
    private String eventId;
    
    @Indexed
    private String eventType;
    
    @Indexed
    private String userId;
    
    private String sessionId;
    
    @Indexed
    private LocalDateTime timestamp;
    
    private String eventData; // JSON string
    
    @Indexed
    private String source;
    
    @Indexed
    private String severity;
    
    @Indexed
    private String category;
    
    // Constructors
    public AuditEvent() {}
    
    public AuditEvent(String eventType, String userId, String eventData) {
        this.eventType = eventType;
        this.userId = userId;
        this.eventData = eventData;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEventData() {
        return eventData;
    }
    
    public void setEventData(String eventData) {
        this.eventData = eventData;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    @Override
    public String toString() {
        return "AuditEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                ", severity='" + severity + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}