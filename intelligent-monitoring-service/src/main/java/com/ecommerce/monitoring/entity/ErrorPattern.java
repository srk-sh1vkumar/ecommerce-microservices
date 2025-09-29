package com.ecommerce.monitoring.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "error_patterns")
public class ErrorPattern {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String signature; // Unique error signature hash
    
    @Indexed
    private String errorType; // "NullPointerException", "SQLException", etc.
    
    @Indexed
    private String serviceName;
    
    private String className;
    private String methodName;
    private int lineNumber;
    
    private String stackTracePattern;
    private List<String> commonCauses;
    private List<String> suggestedFixes;
    
    private int occurrenceCount;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    
    @Indexed
    private String severity; // "critical", "high", "medium", "low"
    
    private boolean hasAutomatedFix;
    private String fixTemplate;
    private String fixDescription;
    
    private Map<String, Object> contextPatterns;
    private List<String> relatedPatterns;
    
    private double confidenceScore; // 0.0 to 1.0
    private boolean validated;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Constructors
    public ErrorPattern() {}
    
    public ErrorPattern(String signature, String errorType, String serviceName) {
        this.signature = signature;
        this.errorType = errorType;
        this.serviceName = serviceName;
        this.occurrenceCount = 1;
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.confidenceScore = 0.5;
        this.validated = false;
    }
    
    // Business methods
    public void incrementOccurrence() {
        this.occurrenceCount++;
        this.lastSeen = LocalDateTime.now();
    }
    
    public void updateConfidenceScore(double newScore) {
        this.confidenceScore = Math.max(0.0, Math.min(1.0, newScore));
    }
    
    public boolean isHighConfidence() {
        return confidenceScore >= 0.8;
    }
    
    public boolean isCritical() {
        return "critical".equals(severity) || "high".equals(severity);
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    
    public String getStackTracePattern() { return stackTracePattern; }
    public void setStackTracePattern(String stackTracePattern) { this.stackTracePattern = stackTracePattern; }
    
    public List<String> getCommonCauses() { return commonCauses; }
    public void setCommonCauses(List<String> commonCauses) { this.commonCauses = commonCauses; }
    
    public List<String> getSuggestedFixes() { return suggestedFixes; }
    public void setSuggestedFixes(List<String> suggestedFixes) { this.suggestedFixes = suggestedFixes; }
    
    public int getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(int occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }
    
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public boolean isHasAutomatedFix() { return hasAutomatedFix; }
    public void setHasAutomatedFix(boolean hasAutomatedFix) { this.hasAutomatedFix = hasAutomatedFix; }
    
    public String getFixTemplate() { return fixTemplate; }
    public void setFixTemplate(String fixTemplate) { this.fixTemplate = fixTemplate; }
    
    public String getFixDescription() { return fixDescription; }
    public void setFixDescription(String fixDescription) { this.fixDescription = fixDescription; }
    
    public Map<String, Object> getContextPatterns() { return contextPatterns; }
    public void setContextPatterns(Map<String, Object> contextPatterns) { this.contextPatterns = contextPatterns; }
    
    public List<String> getRelatedPatterns() { return relatedPatterns; }
    public void setRelatedPatterns(List<String> relatedPatterns) { this.relatedPatterns = relatedPatterns; }
    
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public boolean isValidated() { return validated; }
    public void setValidated(boolean validated) { this.validated = validated; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}