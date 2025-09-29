package com.ecommerce.monitoring.service;

import com.ecommerce.monitoring.entity.AutomatedFix;
import com.ecommerce.monitoring.entity.MonitoringEvent;
import com.ecommerce.monitoring.repository.AutomatedFixRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Human-in-the-loop service for reviewing and approving automated code changes
 * Ensures human oversight for all automated fixes before deployment
 */
@Service
public class HumanReviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(HumanReviewService.class);
    
    @Value("${monitoring.human-review.enabled:true}")
    private boolean humanReviewEnabled;
    
    @Value("${monitoring.human-review.auto-approve-timeout-hours:24}")
    private int autoApproveTimeoutHours;
    
    @Value("${monitoring.human-review.require-multiple-reviewers:false}")
    private boolean requireMultipleReviewers;
    
    @Value("${monitoring.human-review.critical-severity-requires-approval:true}")
    private boolean criticalSeverityRequiresApproval;
    
    private final AutomatedFixRepository automatedFixRepository;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    
    // In-memory storage for pending reviews (could be moved to database for persistence)
    private final Map<String, PendingReview> pendingReviews = new ConcurrentHashMap<>();
    
    public HumanReviewService(AutomatedFixRepository automatedFixRepository,
                            NotificationService notificationService,
                            WebSocketService webSocketService,
                            AuditService auditService) {
        this.automatedFixRepository = automatedFixRepository;
        this.notificationService = notificationService;
        this.webSocketService = webSocketService;
        this.auditService = auditService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Submit a code change for human review
     */
    public ReviewSubmissionResult submitForReview(MonitoringEvent errorEvent, 
                                                 ProposedCodeFix proposedFix,
                                                 String submittedBy) {
        
        if (!humanReviewEnabled) {
            logger.info("Human review disabled, auto-approving fix for: {}", errorEvent.getErrorSignature());
            return new ReviewSubmissionResult(true, "Auto-approved (human review disabled)", null);
        }
        
        String reviewId = generateReviewId();
        
        PendingReview review = new PendingReview();
        review.setReviewId(reviewId);
        review.setErrorEvent(errorEvent);
        review.setProposedFix(proposedFix);
        review.setSubmittedBy(submittedBy);
        review.setSubmittedAt(LocalDateTime.now());
        review.setStatus(ReviewStatus.PENDING);
        review.setSeverity(determineSeverity(errorEvent, proposedFix));
        review.setRequiresApproval(requiresHumanApproval(review));
        
        // Store the pending review
        pendingReviews.put(reviewId, review);
        
        // Create automated fix record with pending status
        AutomatedFix automatedFix = createPendingAutomatedFix(review);
        automatedFixRepository.save(automatedFix);
        
        // Send notifications to reviewers
        sendReviewNotifications(review);
        
        // Audit log the submission
        auditService.logEvent("code_fix_submitted_for_review", Map.of(
            "reviewId", reviewId,
            "errorSignature", errorEvent.getErrorSignature(),
            "service", errorEvent.getService(),
            "severity", review.getSeverity().toString(),
            "submittedBy", submittedBy,
            "requiresApproval", review.isRequiresApproval()
        ));
        
        logger.info("Code fix submitted for review: {} (Severity: {}, Requires Approval: {})", 
                   reviewId, review.getSeverity(), review.isRequiresApproval());
        
        return new ReviewSubmissionResult(false, "Submitted for human review", reviewId);
    }
    
    /**
     * Human reviewer approves the proposed fix
     */
    public ReviewDecisionResult approveReview(String reviewId, String reviewedBy, 
                                            String approvalComments, 
                                            Map<String, Object> modifications) {
        
        PendingReview review = pendingReviews.get(reviewId);
        if (review == null) {
            return new ReviewDecisionResult(false, "Review not found", reviewId);
        }
        
        if (review.getStatus() != ReviewStatus.PENDING) {
            return new ReviewDecisionResult(false, "Review already processed", reviewId);
        }
        
        // Apply human modifications to the proposed fix
        ProposedCodeFix finalFix = applyHumanModifications(review.getProposedFix(), modifications);
        
        // Create review decision
        ReviewDecision decision = new ReviewDecision();
        decision.setReviewedBy(reviewedBy);
        decision.setReviewedAt(LocalDateTime.now());
        decision.setDecision(ReviewDecisionType.APPROVED);
        decision.setComments(approvalComments);
        decision.setModifications(modifications);
        
        review.getReviewDecisions().add(decision);
        
        // Check if we have enough approvals
        if (hasRequiredApprovals(review)) {
            review.setStatus(ReviewStatus.APPROVED);
            review.setFinalFix(finalFix);
            
            // Notify that the fix can now be applied
            notifyFixApproved(review);
            
            // Audit log the approval
            auditService.logEvent("code_fix_approved", Map.of(
                "reviewId", reviewId,
                "reviewedBy", reviewedBy,
                "errorSignature", review.getErrorEvent().getErrorSignature(),
                "hasModifications", !modifications.isEmpty(),
                "approvalComments", approvalComments
            ));
            
            logger.info("Code fix approved: {} by {} (Final approval: {})", 
                       reviewId, reviewedBy, hasRequiredApprovals(review));
            
            return new ReviewDecisionResult(true, "Fix approved and ready for deployment", reviewId);
        } else {
            logger.info("Code fix partially approved: {} by {} (Need more approvals)", 
                       reviewId, reviewedBy);
            
            return new ReviewDecisionResult(true, "Approval recorded, waiting for additional approvals", reviewId);
        }
    }
    
    /**
     * Human reviewer rejects the proposed fix
     */
    public ReviewDecisionResult rejectReview(String reviewId, String reviewedBy, 
                                           String rejectionReason, 
                                           List<String> improvementSuggestions) {
        
        PendingReview review = pendingReviews.get(reviewId);
        if (review == null) {
            return new ReviewDecisionResult(false, "Review not found", reviewId);
        }
        
        if (review.getStatus() != ReviewStatus.PENDING) {
            return new ReviewDecisionResult(false, "Review already processed", reviewId);
        }
        
        // Create review decision
        ReviewDecision decision = new ReviewDecision();
        decision.setReviewedBy(reviewedBy);
        decision.setReviewedAt(LocalDateTime.now());
        decision.setDecision(ReviewDecisionType.REJECTED);
        decision.setComments(rejectionReason);
        decision.setImprovementSuggestions(improvementSuggestions);
        
        review.getReviewDecisions().add(decision);
        review.setStatus(ReviewStatus.REJECTED);
        
        // Notify rejection
        notifyFixRejected(review, rejectionReason, improvementSuggestions);
        
        // Update automated fix record
        updateAutomatedFixStatus(reviewId, "REJECTED", rejectionReason);
        
        // Audit log the rejection
        auditService.logEvent("code_fix_rejected", Map.of(
            "reviewId", reviewId,
            "reviewedBy", reviewedBy,
            "errorSignature", review.getErrorEvent().getErrorSignature(),
            "rejectionReason", rejectionReason,
            "improvementSuggestions", improvementSuggestions
        ));
        
        logger.info("Code fix rejected: {} by {} - Reason: {}", 
                   reviewId, reviewedBy, rejectionReason);
        
        return new ReviewDecisionResult(true, "Fix rejected with feedback", reviewId);
    }
    
    /**
     * Request modifications to the proposed fix
     */
    public ReviewDecisionResult requestModifications(String reviewId, String reviewedBy, 
                                                   String modificationRequest,
                                                   Map<String, Object> suggestedChanges) {
        
        PendingReview review = pendingReviews.get(reviewId);
        if (review == null) {
            return new ReviewDecisionResult(false, "Review not found", reviewId);
        }
        
        // Create review decision
        ReviewDecision decision = new ReviewDecision();
        decision.setReviewedBy(reviewedBy);
        decision.setReviewedAt(LocalDateTime.now());
        decision.setDecision(ReviewDecisionType.MODIFICATIONS_REQUESTED);
        decision.setComments(modificationRequest);
        decision.setSuggestedChanges(suggestedChanges);
        
        review.getReviewDecisions().add(decision);
        review.setStatus(ReviewStatus.MODIFICATIONS_REQUESTED);
        
        // Notify about modification request
        notifyModificationsRequested(review, modificationRequest, suggestedChanges);
        
        // Audit log the modification request
        auditService.logEvent("code_fix_modifications_requested", Map.of(
            "reviewId", reviewId,
            "reviewedBy", reviewedBy,
            "errorSignature", review.getErrorEvent().getErrorSignature(),
            "modificationRequest", modificationRequest,
            "suggestedChanges", suggestedChanges
        ));
        
        logger.info("Code fix modifications requested: {} by {} - Request: {}", 
                   reviewId, reviewedBy, modificationRequest);
        
        return new ReviewDecisionResult(true, "Modifications requested", reviewId);
    }
    
    /**
     * Get all pending reviews for human reviewers
     */
    public List<PendingReview> getPendingReviews() {
        return pendingReviews.values().stream()
                .filter(review -> review.getStatus() == ReviewStatus.PENDING || 
                                review.getStatus() == ReviewStatus.MODIFICATIONS_REQUESTED)
                .sorted((a, b) -> {
                    // Sort by severity (critical first) then by submission time
                    int severityCompare = b.getSeverity().compareTo(a.getSeverity());
                    if (severityCompare != 0) return severityCompare;
                    return a.getSubmittedAt().compareTo(b.getSubmittedAt());
                })
                .toList();
    }
    
    /**
     * Get review details by ID
     */
    public Optional<PendingReview> getReviewById(String reviewId) {
        return Optional.ofNullable(pendingReviews.get(reviewId));
    }
    
    /**
     * Get review history for audit purposes
     */
    public List<PendingReview> getReviewHistory(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return pendingReviews.values().stream()
                .filter(review -> review.getSubmittedAt().isAfter(cutoff))
                .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
                .toList();
    }
    
    /**
     * Auto-approve reviews that have been pending too long
     */
    public void processTimeoutedReviews() {
        LocalDateTime timeout = LocalDateTime.now().minusHours(autoApproveTimeoutHours);
        
        List<PendingReview> timeoutedReviews = pendingReviews.values().stream()
                .filter(review -> review.getStatus() == ReviewStatus.PENDING)
                .filter(review -> review.getSubmittedAt().isBefore(timeout))
                .filter(review -> !review.isRequiresApproval()) // Only auto-approve non-critical fixes
                .toList();
        
        for (PendingReview review : timeoutedReviews) {
            logger.info("Auto-approving timed-out review: {} (submitted: {})", 
                       review.getReviewId(), review.getSubmittedAt());
            
            ReviewDecision autoDecision = new ReviewDecision();
            autoDecision.setReviewedBy("SYSTEM_AUTO_APPROVAL");
            autoDecision.setReviewedAt(LocalDateTime.now());
            autoDecision.setDecision(ReviewDecisionType.AUTO_APPROVED);
            autoDecision.setComments("Auto-approved due to timeout after " + autoApproveTimeoutHours + " hours");
            
            review.getReviewDecisions().add(autoDecision);
            review.setStatus(ReviewStatus.APPROVED);
            review.setFinalFix(review.getProposedFix()); // Use original fix without modifications
            
            notifyFixApproved(review);
            
            auditService.logEvent("code_fix_auto_approved", Map.of(
                "reviewId", review.getReviewId(),
                "timeoutHours", autoApproveTimeoutHours,
                "errorSignature", review.getErrorEvent().getErrorSignature()
            ));
        }
    }
    
    // Private helper methods
    
    private String generateReviewId() {
        return "REVIEW_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private ReviewSeverity determineSeverity(MonitoringEvent errorEvent, ProposedCodeFix proposedFix) {
        // Determine severity based on error impact and fix complexity
        String errorSeverity = errorEvent.getSeverity();
        int impactScore = calculateImpactScore(errorEvent);
        int complexityScore = calculateFixComplexity(proposedFix);
        
        if ("critical".equals(errorSeverity) || impactScore >= 8 || complexityScore >= 8) {
            return ReviewSeverity.CRITICAL;
        } else if ("high".equals(errorSeverity) || impactScore >= 6 || complexityScore >= 6) {
            return ReviewSeverity.HIGH;
        } else if ("medium".equals(errorSeverity) || impactScore >= 4 || complexityScore >= 4) {
            return ReviewSeverity.MEDIUM;
        } else {
            return ReviewSeverity.LOW;
        }
    }
    
    private int calculateImpactScore(MonitoringEvent errorEvent) {
        int score = 0;
        
        // Frequency of error
        Integer count = (Integer) errorEvent.getMetrics().get("count");
        if (count != null) {
            if (count > 100) score += 3;
            else if (count > 50) score += 2;
            else if (count > 10) score += 1;
        }
        
        // Services affected
        if (errorEvent.getService() != null) {
            if (errorEvent.getService().contains("gateway") || 
                errorEvent.getService().contains("auth")) {
                score += 3; // Critical services
            } else if (errorEvent.getService().contains("user") || 
                      errorEvent.getService().contains("order")) {
                score += 2; // Important services
            } else {
                score += 1; // Other services
            }
        }
        
        // Error type
        if (errorEvent.getErrorSignature() != null) {
            String signature = errorEvent.getErrorSignature().toLowerCase();
            if (signature.contains("nullpointer") || signature.contains("security") || 
                signature.contains("authentication")) {
                score += 2;
            }
        }
        
        return Math.min(score, 10); // Cap at 10
    }
    
    private int calculateFixComplexity(ProposedCodeFix proposedFix) {
        int score = 0;
        
        // Number of files being modified
        score += Math.min(proposedFix.getFilesModified().size(), 5);
        
        // Lines of code changed
        int totalLines = proposedFix.getFilesModified().values().stream()
                .mapToInt(mod -> mod.getLinesAdded() + mod.getLinesRemoved())
                .sum();
        
        if (totalLines > 100) score += 3;
        else if (totalLines > 50) score += 2;
        else if (totalLines > 10) score += 1;
        
        // Type of changes
        boolean hasConfigChanges = proposedFix.getFilesModified().keySet().stream()
                .anyMatch(file -> file.endsWith(".yml") || file.endsWith(".properties") || 
                         file.endsWith(".xml"));
        if (hasConfigChanges) score += 1;
        
        boolean hasTestChanges = proposedFix.getTestCases() != null && 
                               !proposedFix.getTestCases().isEmpty();
        if (!hasTestChanges) score += 2; // Higher complexity if no tests provided
        
        return Math.min(score, 10); // Cap at 10
    }
    
    private boolean requiresHumanApproval(PendingReview review) {
        // Always require approval for critical severity
        if (criticalSeverityRequiresApproval && review.getSeverity() == ReviewSeverity.CRITICAL) {
            return true;
        }
        
        // Require approval for high-impact changes
        MonitoringEvent error = review.getErrorEvent();
        if (error.getService() != null && 
            (error.getService().contains("gateway") || error.getService().contains("auth"))) {
            return true;
        }
        
        // Require approval for complex fixes
        int complexity = calculateFixComplexity(review.getProposedFix());
        if (complexity >= 7) {
            return true;
        }
        
        return false;
    }
    
    private boolean hasRequiredApprovals(PendingReview review) {
        List<ReviewDecision> approvals = review.getReviewDecisions().stream()
                .filter(decision -> decision.getDecision() == ReviewDecisionType.APPROVED)
                .toList();
        
        if (requireMultipleReviewers && review.getSeverity() == ReviewSeverity.CRITICAL) {
            return approvals.size() >= 2; // Require 2 approvals for critical fixes
        }
        
        return approvals.size() >= 1; // Require 1 approval for other fixes
    }
    
    private ProposedCodeFix applyHumanModifications(ProposedCodeFix originalFix, 
                                                   Map<String, Object> modifications) {
        if (modifications == null || modifications.isEmpty()) {
            return originalFix;
        }
        
        // Clone the original fix and apply modifications
        ProposedCodeFix modifiedFix = originalFix.deepCopy();
        
        // Apply test case modifications
        if (modifications.containsKey("additionalTestCases")) {
            List<TestCase> additionalTests = (List<TestCase>) modifications.get("additionalTestCases");
            modifiedFix.getTestCases().addAll(additionalTests);
        }
        
        // Apply code modifications
        if (modifications.containsKey("codeModifications")) {
            Map<String, Object> codeModifications = (Map<String, Object>) modifications.get("codeModifications");
            // Apply specific code changes as suggested by human reviewer
            applyCodeModifications(modifiedFix, codeModifications);
        }
        
        // Apply additional safety checks
        if (modifications.containsKey("additionalSafetyChecks")) {
            List<String> safetyChecks = (List<String>) modifications.get("additionalSafetyChecks");
            modifiedFix.setSafetyChecks(safetyChecks);
        }
        
        return modifiedFix;
    }
    
    private void applyCodeModifications(ProposedCodeFix fix, Map<String, Object> modifications) {
        // Implementation would apply specific code changes suggested by human reviewer
        logger.info("Applying human code modifications to fix: {}", modifications);
    }
    
    private AutomatedFix createPendingAutomatedFix(PendingReview review) {
        AutomatedFix fix = new AutomatedFix();
        fix.setErrorPatternId(review.getErrorEvent().getErrorSignature());
        fix.setService(review.getErrorEvent().getService());
        fix.setFixDescription("Pending human review: " + review.getProposedFix().getDescription());
        fix.setStatus("PENDING_REVIEW");
        fix.setCreatedAt(LocalDateTime.now());
        fix.setReviewId(review.getReviewId());
        return fix;
    }
    
    private void updateAutomatedFixStatus(String reviewId, String status, String notes) {
        automatedFixRepository.findByReviewId(reviewId).ifPresent(fix -> {
            fix.setStatus(status);
            fix.setNotes(notes);
            fix.setUpdatedAt(LocalDateTime.now());
            automatedFixRepository.save(fix);
        });
    }
    
    private void sendReviewNotifications(PendingReview review) {
        // Send email notifications to human reviewers
        String subject = String.format("Code Fix Review Required: %s (%s)", 
                                      review.getErrorEvent().getErrorSignature(),
                                      review.getSeverity());
        
        String message = String.format(
            "A new code fix requires human review:\n\n" +
            "Review ID: %s\n" +
            "Error: %s\n" +
            "Service: %s\n" +
            "Severity: %s\n" +
            "Requires Approval: %s\n" +
            "Submitted By: %s\n" +
            "Submitted At: %s\n\n" +
            "Please review at: /admin/code-reviews/%s",
            review.getReviewId(),
            review.getErrorEvent().getErrorSignature(),
            review.getErrorEvent().getService(),
            review.getSeverity(),
            review.isRequiresApproval(),
            review.getSubmittedBy(),
            review.getSubmittedAt(),
            review.getReviewId()
        );
        
        notificationService.sendEmail("code-reviewers@company.com", subject, message);
        
        // Send real-time notification via WebSocket
        Map<String, Object> notification = Map.of(
            "type", "code_review_required",
            "reviewId", review.getReviewId(),
            "errorSignature", review.getErrorEvent().getErrorSignature(),
            "severity", review.getSeverity(),
            "requiresApproval", review.isRequiresApproval()
        );
        
        webSocketService.broadcast("admin-notifications", notification);
    }
    
    private void notifyFixApproved(PendingReview review) {
        notificationService.sendEmail("code-reviewers@company.com", 
            "Code Fix Approved: " + review.getReviewId(),
            "The code fix has been approved and is ready for deployment.");
        
        Map<String, Object> notification = Map.of(
            "type", "code_fix_approved",
            "reviewId", review.getReviewId(),
            "errorSignature", review.getErrorEvent().getErrorSignature()
        );
        
        webSocketService.broadcast("admin-notifications", notification);
    }
    
    private void notifyFixRejected(PendingReview review, String reason, List<String> suggestions) {
        notificationService.sendEmail("code-reviewers@company.com",
            "Code Fix Rejected: " + review.getReviewId(),
            "The code fix has been rejected. Reason: " + reason);
        
        Map<String, Object> notification = Map.of(
            "type", "code_fix_rejected",
            "reviewId", review.getReviewId(),
            "reason", reason,
            "suggestions", suggestions
        );
        
        webSocketService.broadcast("admin-notifications", notification);
    }
    
    private void notifyModificationsRequested(PendingReview review, String request, 
                                            Map<String, Object> suggestedChanges) {
        notificationService.sendEmail("code-reviewers@company.com",
            "Code Fix Modifications Requested: " + review.getReviewId(),
            "Modifications requested for the code fix: " + request);
        
        Map<String, Object> notification = Map.of(
            "type", "code_fix_modifications_requested",
            "reviewId", review.getReviewId(),
            "request", request,
            "suggestedChanges", suggestedChanges
        );
        
        webSocketService.broadcast("admin-notifications", notification);
    }
    
    // Inner classes for data structures
    
    public static class PendingReview {
        private String reviewId;
        private MonitoringEvent errorEvent;
        private ProposedCodeFix proposedFix;
        private ProposedCodeFix finalFix;
        private String submittedBy;
        private LocalDateTime submittedAt;
        private ReviewStatus status;
        private ReviewSeverity severity;
        private boolean requiresApproval;
        private List<ReviewDecision> reviewDecisions = new ArrayList<>();
        
        // Getters and setters
        public String getReviewId() { return reviewId; }
        public void setReviewId(String reviewId) { this.reviewId = reviewId; }
        
        public MonitoringEvent getErrorEvent() { return errorEvent; }
        public void setErrorEvent(MonitoringEvent errorEvent) { this.errorEvent = errorEvent; }
        
        public ProposedCodeFix getProposedFix() { return proposedFix; }
        public void setProposedFix(ProposedCodeFix proposedFix) { this.proposedFix = proposedFix; }
        
        public ProposedCodeFix getFinalFix() { return finalFix; }
        public void setFinalFix(ProposedCodeFix finalFix) { this.finalFix = finalFix; }
        
        public String getSubmittedBy() { return submittedBy; }
        public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
        
        public LocalDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
        
        public ReviewStatus getStatus() { return status; }
        public void setStatus(ReviewStatus status) { this.status = status; }
        
        public ReviewSeverity getSeverity() { return severity; }
        public void setSeverity(ReviewSeverity severity) { this.severity = severity; }
        
        public boolean isRequiresApproval() { return requiresApproval; }
        public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
        
        public List<ReviewDecision> getReviewDecisions() { return reviewDecisions; }
        public void setReviewDecisions(List<ReviewDecision> reviewDecisions) { this.reviewDecisions = reviewDecisions; }
    }
    
    public static class ReviewDecision {
        private String reviewedBy;
        private LocalDateTime reviewedAt;
        private ReviewDecisionType decision;
        private String comments;
        private Map<String, Object> modifications;
        private Map<String, Object> suggestedChanges;
        private List<String> improvementSuggestions;
        
        // Getters and setters
        public String getReviewedBy() { return reviewedBy; }
        public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
        
        public LocalDateTime getReviewedAt() { return reviewedAt; }
        public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
        
        public ReviewDecisionType getDecision() { return decision; }
        public void setDecision(ReviewDecisionType decision) { this.decision = decision; }
        
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        
        public Map<String, Object> getModifications() { return modifications; }
        public void setModifications(Map<String, Object> modifications) { this.modifications = modifications; }
        
        public Map<String, Object> getSuggestedChanges() { return suggestedChanges; }
        public void setSuggestedChanges(Map<String, Object> suggestedChanges) { this.suggestedChanges = suggestedChanges; }
        
        public List<String> getImprovementSuggestions() { return improvementSuggestions; }
        public void setImprovementSuggestions(List<String> improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }
    }
    
    public static class ReviewSubmissionResult {
        private final boolean autoApproved;
        private final String message;
        private final String reviewId;
        
        public ReviewSubmissionResult(boolean autoApproved, String message, String reviewId) {
            this.autoApproved = autoApproved;
            this.message = message;
            this.reviewId = reviewId;
        }
        
        public boolean isAutoApproved() { return autoApproved; }
        public String getMessage() { return message; }
        public String getReviewId() { return reviewId; }
    }
    
    public static class ReviewDecisionResult {
        private final boolean success;
        private final String message;
        private final String reviewId;
        
        public ReviewDecisionResult(boolean success, String message, String reviewId) {
            this.success = success;
            this.message = message;
            this.reviewId = reviewId;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getReviewId() { return reviewId; }
    }
    
    public enum ReviewStatus {
        PENDING,
        APPROVED,
        REJECTED,
        MODIFICATIONS_REQUESTED,
        AUTO_APPROVED
    }
    
    public enum ReviewSeverity {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int priority;
        
        ReviewSeverity(int priority) {
            this.priority = priority;
        }
        
        public int getPriority() { return priority; }
    }
    
    public enum ReviewDecisionType {
        APPROVED,
        REJECTED,
        MODIFICATIONS_REQUESTED,
        AUTO_APPROVED
    }
    
    // Supporting classes
    
    public static class ProposedCodeFix {
        private String description;
        private String fixType;
        private Map<String, FileModification> filesModified;
        private List<TestCase> testCases;
        private List<String> safetyChecks;
        private String rollbackPlan;
        private int confidenceScore;
        
        public ProposedCodeFix deepCopy() {
            ProposedCodeFix copy = new ProposedCodeFix();
            copy.description = this.description;
            copy.fixType = this.fixType;
            copy.filesModified = new HashMap<>(this.filesModified);
            copy.testCases = new ArrayList<>(this.testCases);
            copy.safetyChecks = new ArrayList<>(this.safetyChecks);
            copy.rollbackPlan = this.rollbackPlan;
            copy.confidenceScore = this.confidenceScore;
            return copy;
        }
        
        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getFixType() { return fixType; }
        public void setFixType(String fixType) { this.fixType = fixType; }
        
        public Map<String, FileModification> getFilesModified() { return filesModified; }
        public void setFilesModified(Map<String, FileModification> filesModified) { this.filesModified = filesModified; }
        
        public List<TestCase> getTestCases() { return testCases; }
        public void setTestCases(List<TestCase> testCases) { this.testCases = testCases; }
        
        public List<String> getSafetyChecks() { return safetyChecks; }
        public void setSafetyChecks(List<String> safetyChecks) { this.safetyChecks = safetyChecks; }
        
        public String getRollbackPlan() { return rollbackPlan; }
        public void setRollbackPlan(String rollbackPlan) { this.rollbackPlan = rollbackPlan; }
        
        public int getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
    }
    
    public static class FileModification {
        private String filePath;
        private String originalContent;
        private String modifiedContent;
        private int linesAdded;
        private int linesRemoved;
        private String changeType; // ADD, MODIFY, DELETE
        
        // Getters and setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public String getOriginalContent() { return originalContent; }
        public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }
        
        public String getModifiedContent() { return modifiedContent; }
        public void setModifiedContent(String modifiedContent) { this.modifiedContent = modifiedContent; }
        
        public int getLinesAdded() { return linesAdded; }
        public void setLinesAdded(int linesAdded) { this.linesAdded = linesAdded; }
        
        public int getLinesRemoved() { return linesRemoved; }
        public void setLinesRemoved(int linesRemoved) { this.linesRemoved = linesRemoved; }
        
        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }
    }
    
    public static class TestCase {
        private String name;
        private String description;
        private String testCode;
        private String expectedResult;
        private boolean isIntegrationTest;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getTestCode() { return testCode; }
        public void setTestCode(String testCode) { this.testCode = testCode; }
        
        public String getExpectedResult() { return expectedResult; }
        public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
        
        public boolean isIntegrationTest() { return isIntegrationTest; }
        public void setIntegrationTest(boolean integrationTest) { isIntegrationTest = integrationTest; }
    }
}