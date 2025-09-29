package com.ecommerce.monitoring.service;

import com.ecommerce.monitoring.entity.ErrorPattern;
import com.ecommerce.monitoring.entity.MonitoringEvent;
import com.ecommerce.monitoring.repository.ErrorPatternRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ErrorPatternAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorPatternAnalysisService.class);
    
    private final ErrorPatternRepository errorPatternRepository;
    private final AutomatedFixingService automatedFixingService;
    
    // Common error patterns and their fixes
    private static final Map<String, ErrorPatternTemplate> ERROR_TEMPLATES = new HashMap<>();
    
    static {
        // NullPointerException patterns
        ERROR_TEMPLATES.put("NullPointerException", new ErrorPatternTemplate(
            "NullPointerException",
            Arrays.asList("Missing null check", "Uninitialized object", "Optional not used"),
            Arrays.asList("Add null checks", "Use Optional", "Initialize objects properly"),
            "Add null safety checks and Optional usage",
            true
        ));
        
        // SQLException patterns
        ERROR_TEMPLATES.put("SQLException", new ErrorPatternTemplate(
            "SQLException",
            Arrays.asList("Connection timeout", "Database unavailable", "Query syntax error"),
            Arrays.asList("Add @Retryable", "Implement circuit breaker", "Add connection pooling"),
            "Implement database resilience patterns",
            true
        ));
        
        // RestTemplate/HTTP errors
        ERROR_TEMPLATES.put("RestClientException", new ErrorPatternTemplate(
            "RestClientException",
            Arrays.asList("Service unavailable", "Timeout", "Connection refused"),
            Arrays.asList("Add circuit breaker", "Implement retry logic", "Add fallback"),
            "Implement HTTP client resilience",
            true
        ));
        
        // Memory issues
        ERROR_TEMPLATES.put("OutOfMemoryError", new ErrorPatternTemplate(
            "OutOfMemoryError",
            Arrays.asList("Heap space", "Memory leak", "Large object allocation"),
            Arrays.asList("Optimize memory usage", "Add resource cleanup", "Implement pagination"),
            "Optimize memory management",
            false // Requires manual analysis
        ));
        
        // Spring configuration errors
        ERROR_TEMPLATES.put("BeanCreationException", new ErrorPatternTemplate(
            "BeanCreationException",
            Arrays.asList("Missing bean", "Circular dependency", "Configuration error"),
            Arrays.asList("Add @Component", "Fix circular dependencies", "Review configuration"),
            "Fix Spring configuration issues",
            true
        ));
    }
    
    public ErrorPatternAnalysisService(ErrorPatternRepository errorPatternRepository,
                                     AutomatedFixingService automatedFixingService) {
        this.errorPatternRepository = errorPatternRepository;
        this.automatedFixingService = automatedFixingService;
    }
    
    /**
     * Analyze error event and update or create error pattern
     */
    public void analyzeErrorPattern(MonitoringEvent errorEvent) {
        try {
            String signature = generateErrorSignature(errorEvent);
            
            Optional<ErrorPattern> existingPattern = errorPatternRepository.findBySignature(signature);
            
            if (existingPattern.isPresent()) {
                updateExistingPattern(existingPattern.get(), errorEvent);
            } else {
                createNewPattern(signature, errorEvent);
            }
            
        } catch (Exception e) {
            logger.error("Error analyzing error pattern for event: {}", errorEvent.getId(), e);
        }
    }
    
    /**
     * Generate unique signature for error based on stack trace and context
     */
    private String generateErrorSignature(MonitoringEvent event) {
        StringBuilder signatureInput = new StringBuilder();
        
        // Include service name
        signatureInput.append(event.getServiceName()).append("|");
        
        // Extract error type from stack trace or error signature
        String errorType = extractErrorType(event);
        signatureInput.append(errorType).append("|");
        
        // Extract method and class from stack trace
        String codeLocation = extractCodeLocation(event);
        signatureInput.append(codeLocation).append("|");
        
        // Include relevant parts of stack trace (first few frames)
        String relevantStackTrace = extractRelevantStackTrace(event.getStackTrace());
        signatureInput.append(relevantStackTrace);
        
        // Generate MD5 hash of the signature
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(signatureInput.toString().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            // Fallback to simple hash
            return Integer.toHexString(signatureInput.toString().hashCode());
        }
    }
    
    /**
     * Extract error type from stack trace or event
     */
    private String extractErrorType(MonitoringEvent event) {
        if (event.getErrorSignature() != null) {
            return event.getErrorSignature();
        }
        
        String stackTrace = event.getStackTrace();
        if (stackTrace != null) {
            // Look for common Java exception patterns
            Pattern exceptionPattern = Pattern.compile("([a-zA-Z.]+Exception|[a-zA-Z.]+Error)");
            Matcher matcher = exceptionPattern.matcher(stackTrace);
            if (matcher.find()) {
                String fullException = matcher.group(1);
                // Return just the simple class name
                return fullException.substring(fullException.lastIndexOf('.') + 1);
            }
        }
        
        return "UnknownError";
    }
    
    /**
     * Extract code location (class.method) from stack trace
     */
    private String extractCodeLocation(MonitoringEvent event) {
        if (event.getCodeLocation() != null) {
            return event.getCodeLocation();
        }
        
        String stackTrace = event.getStackTrace();
        if (stackTrace != null) {
            // Look for service-specific stack trace frames
            String serviceName = event.getServiceName();
            Pattern servicePattern = Pattern.compile("at\\s+([a-zA-Z.]+\\." + serviceName + "[a-zA-Z.]*)\\.([a-zA-Z]+)");
            Matcher matcher = servicePattern.matcher(stackTrace);
            
            if (matcher.find()) {
                String className = matcher.group(1);
                String methodName = matcher.group(2);
                return className + "." + methodName;
            }
            
            // Fallback to first stack trace frame
            Pattern generalPattern = Pattern.compile("at\\s+([a-zA-Z.]+)\\.([a-zA-Z]+)");
            matcher = generalPattern.matcher(stackTrace);
            if (matcher.find()) {
                return matcher.group(1) + "." + matcher.group(2);
            }
        }
        
        return "unknown.method";
    }
    
    /**
     * Extract relevant parts of stack trace for pattern matching
     */
    private String extractRelevantStackTrace(String stackTrace) {
        if (stackTrace == null) {
            return "";
        }
        
        // Take first 3 lines of stack trace and normalize
        String[] lines = stackTrace.split("\n");
        StringBuilder relevant = new StringBuilder();
        
        for (int i = 0; i < Math.min(3, lines.length); i++) {
            String line = lines[i].trim();
            // Normalize line numbers and specific values
            line = line.replaceAll(":[0-9]+", ":XXX"); // Replace line numbers
            line = line.replaceAll("\\$[0-9]+", "$XXX"); // Replace lambda numbers
            relevant.append(line).append("|");
        }
        
        return relevant.toString();
    }
    
    /**
     * Update existing error pattern with new occurrence
     */
    private void updateExistingPattern(ErrorPattern pattern, MonitoringEvent event) {
        pattern.incrementOccurrence();
        
        // Update confidence score based on frequency
        double newConfidence = calculateConfidenceScore(pattern.getOccurrenceCount(), pattern.getFirstSeen());
        pattern.updateConfidenceScore(newConfidence);
        
        // Update severity if needed
        String eventSeverity = event.getSeverity();
        if (isHigherSeverity(eventSeverity, pattern.getSeverity())) {
            pattern.setSeverity(eventSeverity);
        }
        
        errorPatternRepository.save(pattern);
        
        // Trigger automated fix if pattern is ready
        if (shouldTriggerAutomatedFix(pattern)) {
            automatedFixingService.triggerAutomatedFix(pattern);
        }
        
        logger.debug("Updated error pattern {} with occurrence count: {}", 
                    pattern.getSignature(), pattern.getOccurrenceCount());
    }
    
    /**
     * Create new error pattern from event
     */
    private void createNewPattern(String signature, MonitoringEvent event) {
        String errorType = extractErrorType(event);
        ErrorPattern pattern = new ErrorPattern(signature, errorType, event.getServiceName());
        
        // Set basic information
        String codeLocation = extractCodeLocation(event);
        String[] locationParts = codeLocation.split("\\.");
        if (locationParts.length >= 2) {
            pattern.setClassName(locationParts[0]);
            pattern.setMethodName(locationParts[1]);
        }
        
        pattern.setSeverity(event.getSeverity());
        pattern.setStackTracePattern(extractRelevantStackTrace(event.getStackTrace()));
        
        // Apply template if available
        ErrorPatternTemplate template = ERROR_TEMPLATES.get(errorType);
        if (template != null) {
            pattern.setCommonCauses(template.getCommonCauses());
            pattern.setSuggestedFixes(template.getSuggestedFixes());
            pattern.setFixDescription(template.getFixDescription());
            pattern.setHasAutomatedFix(template.isHasAutomatedFix());
        }
        
        // Initial confidence score
        pattern.setConfidenceScore(0.3); // Start with low confidence
        
        errorPatternRepository.save(pattern);
        
        logger.info("Created new error pattern: {} for service: {} with error type: {}", 
                   signature, event.getServiceName(), errorType);
    }
    
    /**
     * Calculate confidence score based on frequency and time
     */
    private double calculateConfidenceScore(int occurrenceCount, LocalDateTime firstSeen) {
        // Base score from occurrence count
        double occurrenceScore = Math.min(occurrenceCount / 10.0, 0.7);
        
        // Time factor - patterns seen over longer periods are more reliable
        long hoursAge = java.time.Duration.between(firstSeen, LocalDateTime.now()).toHours();
        double timeScore = Math.min(hoursAge / 24.0, 0.3); // Max 0.3 for patterns over 24 hours
        
        return Math.min(occurrenceScore + timeScore, 1.0);
    }
    
    /**
     * Check if new severity is higher than current
     */
    private boolean isHigherSeverity(String newSeverity, String currentSeverity) {
        Map<String, Integer> severityRank = Map.of(
            "critical", 4,
            "high", 3,
            "medium", 2,
            "low", 1,
            "info", 0
        );
        
        return severityRank.getOrDefault(newSeverity, 0) > severityRank.getOrDefault(currentSeverity, 0);
    }
    
    /**
     * Determine if pattern is ready for automated fixing
     */
    private boolean shouldTriggerAutomatedFix(ErrorPattern pattern) {
        return pattern.isHasAutomatedFix() &&
               pattern.isHighConfidence() &&
               pattern.getOccurrenceCount() >= 3 &&
               pattern.isCritical();
    }
    
    /**
     * Find similar error patterns for correlation
     */
    public List<ErrorPattern> findSimilarPatterns(ErrorPattern pattern) {
        return errorPatternRepository.findSimilarPatterns(
            pattern.getServiceName(),
            pattern.getErrorType(),
            pattern.getClassName(),
            pattern.getSignature()
        );
    }
    
    /**
     * Get patterns requiring immediate attention
     */
    public List<ErrorPattern> getPatternsRequiringAttention() {
        return errorPatternRepository.findPatternsNeedingAttention();
    }
    
    /**
     * Get fixable patterns ready for automation
     */
    public List<ErrorPattern> getFixablePatterns() {
        return errorPatternRepository.findFixablePatterns(0.8);
    }
    
    /**
     * Validate a pattern (mark as human-verified)
     */
    public void validatePattern(String patternId, boolean isValid) {
        Optional<ErrorPattern> patternOpt = errorPatternRepository.findById(patternId);
        if (patternOpt.isPresent()) {
            ErrorPattern pattern = patternOpt.get();
            pattern.setValidated(isValid);
            if (isValid) {
                pattern.updateConfidenceScore(Math.max(pattern.getConfidenceScore(), 0.9));
            }
            errorPatternRepository.save(pattern);
        }
    }
    
    /**
     * Inner class for error pattern templates
     */
    private static class ErrorPatternTemplate {
        private final String errorType;
        private final List<String> commonCauses;
        private final List<String> suggestedFixes;
        private final String fixDescription;
        private final boolean hasAutomatedFix;
        
        public ErrorPatternTemplate(String errorType, List<String> commonCauses, 
                                  List<String> suggestedFixes, String fixDescription, 
                                  boolean hasAutomatedFix) {
            this.errorType = errorType;
            this.commonCauses = commonCauses;
            this.suggestedFixes = suggestedFixes;
            this.fixDescription = fixDescription;
            this.hasAutomatedFix = hasAutomatedFix;
        }
        
        public List<String> getCommonCauses() { return commonCauses; }
        public List<String> getSuggestedFixes() { return suggestedFixes; }
        public String getFixDescription() { return fixDescription; }
        public boolean isHasAutomatedFix() { return hasAutomatedFix; }
    }
}