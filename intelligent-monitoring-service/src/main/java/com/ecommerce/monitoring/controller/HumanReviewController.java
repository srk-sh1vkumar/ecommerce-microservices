package com.ecommerce.monitoring.controller;

import com.ecommerce.monitoring.service.HumanReviewService;
import com.ecommerce.monitoring.service.HumanReviewService.PendingReview;
import com.ecommerce.monitoring.service.HumanReviewService.ReviewDecisionResult;
import com.ecommerce.monitoring.service.HumanReviewService.ReviewSubmissionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for human review workflow management
 * Enables admin dashboard to interact with automated code fix reviews
 */
@RestController
@RequestMapping("/api/monitoring/human-review")
@CrossOrigin(origins = {"https://admin.ecommerce.com", "http://localhost:4200"})
public class HumanReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(HumanReviewController.class);
    
    private final HumanReviewService humanReviewService;
    
    public HumanReviewController(HumanReviewService humanReviewService) {
        this.humanReviewService = humanReviewService;
    }
    
    /**
     * Get all pending reviews requiring human attention
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingReviews() {
        try {
            List<PendingReview> pendingReviews = humanReviewService.getPendingReviews();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", pendingReviews.size());
            response.put("reviews", pendingReviews);
            response.put("retrievedAt", java.time.LocalDateTime.now());
            
            logger.debug("Retrieved {} pending reviews", pendingReviews.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving pending reviews", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve pending reviews");
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get review details by ID
     */
    @GetMapping("/review/{reviewId}")
    public ResponseEntity<Map<String, Object>> getReviewDetails(@PathVariable String reviewId) {
        try {
            Optional<PendingReview> review = humanReviewService.getReviewById(reviewId);
            
            if (review.isEmpty()) {
                Map<String, Object> notFound = new HashMap<>();
                notFound.put("success", false);
                notFound.put("message", "Review not found");
                notFound.put("reviewId", reviewId);
                
                return ResponseEntity.status(404).body(notFound);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("review", review.get());
            response.put("retrievedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving review details for ID: {}", reviewId, e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve review details");
            error.put("reviewId", reviewId);
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Approve a proposed code fix
     */
    @PostMapping("/review/{reviewId}/approve")
    public ResponseEntity<Map<String, Object>> approveReview(
            @PathVariable String reviewId,
            @RequestBody ApprovalRequest request) {
        
        try {
            String reviewedBy = request.getReviewedBy();
            String comments = request.getComments() != null ? request.getComments() : "";
            Map<String, Object> modifications = request.getModifications() != null ? 
                request.getModifications() : new HashMap<>();
            
            ReviewDecisionResult result = humanReviewService.approveReview(
                reviewId, reviewedBy, comments, modifications);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("reviewId", result.getReviewId());
            response.put("approvedBy", reviewedBy);
            response.put("approvedAt", java.time.LocalDateTime.now());
            
            if (!result.isSuccess()) {
                return ResponseEntity.status(400).body(response);
            }
            
            logger.info("Review {} approved by {}", reviewId, reviewedBy);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error approving review {}", reviewId, e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to approve review");
            error.put("reviewId", reviewId);
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Reject a proposed code fix
     */
    @PostMapping("/review/{reviewId}/reject")
    public ResponseEntity<Map<String, Object>> rejectReview(
            @PathVariable String reviewId,
            @RequestBody RejectionRequest request) {
        
        try {
            String reviewedBy = request.getReviewedBy();
            String rejectionReason = request.getRejectionReason();
            List<String> improvementSuggestions = request.getImprovementSuggestions() != null ? 
                request.getImprovementSuggestions() : List.of();
            
            ReviewDecisionResult result = humanReviewService.rejectReview(
                reviewId, reviewedBy, rejectionReason, improvementSuggestions);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("reviewId", result.getReviewId());
            response.put("rejectedBy", reviewedBy);
            response.put("rejectedAt", java.time.LocalDateTime.now());
            response.put("rejectionReason", rejectionReason);
            
            if (!result.isSuccess()) {
                return ResponseEntity.status(400).body(response);
            }
            
            logger.info("Review {} rejected by {} - Reason: {}", reviewId, reviewedBy, rejectionReason);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error rejecting review {}", reviewId, e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to reject review");
            error.put("reviewId", reviewId);
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Request modifications to a proposed code fix
     */
    @PostMapping("/review/{reviewId}/request-modifications")
    public ResponseEntity<Map<String, Object>> requestModifications(
            @PathVariable String reviewId,
            @RequestBody ModificationRequest request) {
        
        try {
            String reviewedBy = request.getReviewedBy();
            String modificationRequest = request.getModificationRequest();
            Map<String, Object> suggestedChanges = request.getSuggestedChanges() != null ? 
                request.getSuggestedChanges() : new HashMap<>();
            
            ReviewDecisionResult result = humanReviewService.requestModifications(
                reviewId, reviewedBy, modificationRequest, suggestedChanges);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("reviewId", result.getReviewId());
            response.put("requestedBy", reviewedBy);
            response.put("requestedAt", java.time.LocalDateTime.now());
            response.put("modificationRequest", modificationRequest);
            
            if (!result.isSuccess()) {
                return ResponseEntity.status(400).body(response);
            }
            
            logger.info("Review {} modifications requested by {} - Request: {}", 
                       reviewId, reviewedBy, modificationRequest);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error requesting modifications for review {}", reviewId, e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to request modifications");
            error.put("reviewId", reviewId);
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get review history for audit purposes
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getReviewHistory(
            @RequestParam(defaultValue = "30") int days) {
        
        try {
            List<PendingReview> reviewHistory = humanReviewService.getReviewHistory(days);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", reviewHistory.size());
            response.put("reviews", reviewHistory);
            response.put("days", days);
            response.put("retrievedAt", java.time.LocalDateTime.now());
            
            // Aggregate statistics
            Map<String, Object> stats = new HashMap<>();
            long approved = reviewHistory.stream()
                .filter(r -> r.getStatus() == HumanReviewService.ReviewStatus.APPROVED)
                .count();
            long rejected = reviewHistory.stream()
                .filter(r -> r.getStatus() == HumanReviewService.ReviewStatus.REJECTED)
                .count();
            long pending = reviewHistory.stream()
                .filter(r -> r.getStatus() == HumanReviewService.ReviewStatus.PENDING)
                .count();
            long modificationsRequested = reviewHistory.stream()
                .filter(r -> r.getStatus() == HumanReviewService.ReviewStatus.MODIFICATIONS_REQUESTED)
                .count();
            
            stats.put("approved", approved);
            stats.put("rejected", rejected);
            stats.put("pending", pending);
            stats.put("modificationsRequested", modificationsRequested);
            stats.put("total", reviewHistory.size());
            
            response.put("statistics", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving review history for {} days", days, e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve review history");
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get review workflow statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            List<PendingReview> pendingReviews = humanReviewService.getPendingReviews();
            List<PendingReview> recentHistory = humanReviewService.getReviewHistory(7);
            
            Map<String, Object> stats = new HashMap<>();
            
            // Current pending reviews
            stats.put("currentPendingCount", pendingReviews.size());
            
            // Pending by severity
            Map<String, Long> pendingBySeverity = new HashMap<>();
            pendingReviews.stream().forEach(review -> {
                String severity = review.getSeverity().toString();
                pendingBySeverity.put(severity, pendingBySeverity.getOrDefault(severity, 0L) + 1);
            });
            stats.put("pendingBySeverity", pendingBySeverity);
            
            // Recent activity (last 7 days)
            long recentApproved = recentHistory.stream()
                .filter(r -> r.getStatus() == HumanReviewService.ReviewStatus.APPROVED)
                .count();
            long recentRejected = recentHistory.stream()
                .filter(r -> r.getStatus() == HumanReviewService.ReviewStatus.REJECTED)
                .count();
            
            Map<String, Object> recentActivity = new HashMap<>();
            recentActivity.put("approved", recentApproved);
            recentActivity.put("rejected", recentRejected);
            recentActivity.put("total", recentHistory.size());
            recentActivity.put("approvalRate", recentHistory.size() > 0 ? 
                (double) recentApproved / recentHistory.size() * 100 : 0);
            
            stats.put("last7Days", recentActivity);
            
            // Oldest pending review age
            if (!pendingReviews.isEmpty()) {
                PendingReview oldest = pendingReviews.stream()
                    .min((a, b) -> a.getSubmittedAt().compareTo(b.getSubmittedAt()))
                    .get();
                
                long hoursSinceOldest = java.time.Duration.between(
                    oldest.getSubmittedAt(), java.time.LocalDateTime.now()).toHours();
                
                stats.put("oldestPendingHours", hoursSinceOldest);
                stats.put("oldestPendingReviewId", oldest.getReviewId());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);
            response.put("generatedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating statistics", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to generate statistics");
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Process timed-out reviews (manual trigger)
     */
    @PostMapping("/process-timeouts")
    public ResponseEntity<Map<String, Object>> processTimeouts() {
        try {
            logger.info("Manual timeout processing requested");
            
            humanReviewService.processTimeoutedReviews();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Timeout processing completed");
            response.put("processedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing timeouts", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to process timeouts");
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // Request DTOs
    
    public static class ApprovalRequest {
        private String reviewedBy;
        private String comments;
        private Map<String, Object> modifications;
        
        public String getReviewedBy() { return reviewedBy; }
        public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
        
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        
        public Map<String, Object> getModifications() { return modifications; }
        public void setModifications(Map<String, Object> modifications) { this.modifications = modifications; }
    }
    
    public static class RejectionRequest {
        private String reviewedBy;
        private String rejectionReason;
        private List<String> improvementSuggestions;
        
        public String getReviewedBy() { return reviewedBy; }
        public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
        
        public String getRejectionReason() { return rejectionReason; }
        public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
        
        public List<String> getImprovementSuggestions() { return improvementSuggestions; }
        public void setImprovementSuggestions(List<String> improvementSuggestions) { 
            this.improvementSuggestions = improvementSuggestions; 
        }
    }
    
    public static class ModificationRequest {
        private String reviewedBy;
        private String modificationRequest;
        private Map<String, Object> suggestedChanges;
        
        public String getReviewedBy() { return reviewedBy; }
        public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
        
        public String getModificationRequest() { return modificationRequest; }
        public void setModificationRequest(String modificationRequest) { this.modificationRequest = modificationRequest; }
        
        public Map<String, Object> getSuggestedChanges() { return suggestedChanges; }
        public void setSuggestedChanges(Map<String, Object> suggestedChanges) { 
            this.suggestedChanges = suggestedChanges; 
        }
    }
}