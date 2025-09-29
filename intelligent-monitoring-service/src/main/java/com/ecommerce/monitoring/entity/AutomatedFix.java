package com.ecommerce.monitoring.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "automated_fixes")
public class AutomatedFix {
    
    @Id
    private String id;
    
    @Indexed
    private String errorPatternId;
    
    @Indexed
    private String serviceName;
    
    @Indexed
    private String status; // "pending", "applied", "tested", "validated", "failed", "rolled_back"
    
    private String fixType; // "null_check", "circuit_breaker", "retry", "resource_management", etc.
    
    private String originalCode;
    private String fixedCode;
    private String filePath;
    private int lineNumber;
    
    private String commitId;
    private String branchName;
    private String pullRequestUrl;
    
    private String description;
    private List<String> changes;
    private Map<String, Object> impactAnalysis;
    
    private boolean testsPassed;
    private String testResults;
    private boolean performanceImpact;
    private Map<String, Double> performanceMetrics;
    
    private String appliedBy; // "system" or user identifier
    private String validatedBy;
    
    @CreatedDate
    @Indexed(expireAfterSeconds = 7776000) // 90 days TTL
    private LocalDateTime timestamp;
    
    private LocalDateTime appliedAt;
    private LocalDateTime validatedAt;
    private LocalDateTime rolledBackAt;
    
    private String rollbackReason;
    private boolean requiresManualReview;
    
    @Indexed
    private String reviewId; // Link to human review process
    private String notes; // Additional notes or comments
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public AutomatedFix() {}
    
    public AutomatedFix(String errorPatternId, String serviceName, String fixType) {
        this.errorPatternId = errorPatternId;
        this.serviceName = serviceName;
        this.fixType = fixType;
        this.status = "pending";
        this.appliedBy = "system";
        this.timestamp = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business methods
    public void markAsApplied(String commitId, String branchName) {
        this.status = "applied";
        this.commitId = commitId;
        this.branchName = branchName;
        this.appliedAt = LocalDateTime.now();
    }
    
    public void markAsValidated(String validatedBy) {
        this.status = "validated";
        this.validatedBy = validatedBy;
        this.validatedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String reason) {
        this.status = "failed";
        this.rollbackReason = reason;
    }
    
    public void rollback(String reason) {
        this.status = "rolled_back";
        this.rollbackReason = reason;
        this.rolledBackAt = LocalDateTime.now();
    }
    
    public boolean isSuccessful() {
        return "validated".equals(status) && testsPassed;
    }
    
    public boolean needsAttention() {
        return "failed".equals(status) || requiresManualReview;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getErrorPatternId() { return errorPatternId; }
    public void setErrorPatternId(String errorPatternId) { this.errorPatternId = errorPatternId; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getFixType() { return fixType; }
    public void setFixType(String fixType) { this.fixType = fixType; }
    
    public String getOriginalCode() { return originalCode; }
    public void setOriginalCode(String originalCode) { this.originalCode = originalCode; }
    
    public String getFixedCode() { return fixedCode; }
    public void setFixedCode(String fixedCode) { this.fixedCode = fixedCode; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    
    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }
    
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    
    public String getPullRequestUrl() { return pullRequestUrl; }
    public void setPullRequestUrl(String pullRequestUrl) { this.pullRequestUrl = pullRequestUrl; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<String> getChanges() { return changes; }
    public void setChanges(List<String> changes) { this.changes = changes; }
    
    public Map<String, Object> getImpactAnalysis() { return impactAnalysis; }
    public void setImpactAnalysis(Map<String, Object> impactAnalysis) { this.impactAnalysis = impactAnalysis; }
    
    public boolean isTestsPassed() { return testsPassed; }
    public void setTestsPassed(boolean testsPassed) { this.testsPassed = testsPassed; }
    
    public String getTestResults() { return testResults; }
    public void setTestResults(String testResults) { this.testResults = testResults; }
    
    public boolean isPerformanceImpact() { return performanceImpact; }
    public void setPerformanceImpact(boolean performanceImpact) { this.performanceImpact = performanceImpact; }
    
    public Map<String, Double> getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(Map<String, Double> performanceMetrics) { this.performanceMetrics = performanceMetrics; }
    
    public String getAppliedBy() { return appliedBy; }
    public void setAppliedBy(String appliedBy) { this.appliedBy = appliedBy; }
    
    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String validatedBy) { this.validatedBy = validatedBy; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }
    
    public LocalDateTime getRolledBackAt() { return rolledBackAt; }
    public void setRolledBackAt(LocalDateTime rolledBackAt) { this.rolledBackAt = rolledBackAt; }
    
    public String getRollbackReason() { return rollbackReason; }
    public void setRollbackReason(String rollbackReason) { this.rollbackReason = rollbackReason; }
    
    public boolean isRequiresManualReview() { return requiresManualReview; }
    public void setRequiresManualReview(boolean requiresManualReview) { this.requiresManualReview = requiresManualReview; }
    
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}