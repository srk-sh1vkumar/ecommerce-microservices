package com.ecommerce.monitoring.repository;

import com.ecommerce.monitoring.entity.ErrorPattern;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ErrorPatternRepository extends MongoRepository<ErrorPattern, String> {
    
    // Find by unique signature
    Optional<ErrorPattern> findBySignature(String signature);
    
    // Find by error type
    List<ErrorPattern> findByErrorType(String errorType);
    
    // Find by service name
    List<ErrorPattern> findByServiceName(String serviceName);
    
    // Find by service and error type
    List<ErrorPattern> findByServiceNameAndErrorType(String serviceName, String errorType);
    
    // Find by severity
    List<ErrorPattern> findBySeverity(String severity);
    
    // Find patterns with automated fixes
    List<ErrorPattern> findByHasAutomatedFixTrue();
    
    // Find validated patterns
    List<ErrorPattern> findByValidatedTrue();
    
    // Find high confidence patterns
    @Query("{'confidenceScore': {'$gte': ?0}}")
    List<ErrorPattern> findByConfidenceScoreGreaterThanEqual(double confidenceScore);
    
    // Find recent patterns
    @Query("{'lastSeen': {'$gte': ?0}}")
    List<ErrorPattern> findByLastSeenAfter(LocalDateTime timestamp);
    
    // Find active patterns (seen recently and high occurrence)
    @Query("{'lastSeen': {'$gte': ?0}, 'occurrenceCount': {'$gte': ?1}}")
    List<ErrorPattern> findActivePatterns(LocalDateTime since, int minOccurrences);
    
    // Find patterns needing attention (high severity, no automated fix)
    @Query("{'severity': {'$in': ['critical', 'high']}, 'hasAutomatedFix': false}")
    List<ErrorPattern> findPatternsNeedingAttention();
    
    // Find patterns by class and method
    List<ErrorPattern> findByClassNameAndMethodName(String className, String methodName);
    
    // Find patterns by stack trace pattern (regex search)
    @Query("{'stackTracePattern': {'$regex': ?0, '$options': 'i'}}")
    List<ErrorPattern> findByStackTracePatternContaining(String pattern);
    
    // Count patterns by service
    @Query(value = "{'serviceName': ?0}", count = true)
    long countByServiceName(String serviceName);
    
    // Count patterns by error type
    @Query(value = "{'errorType': ?0}", count = true)
    long countByErrorType(String errorType);
    
    // Find trending patterns (increasing occurrence recently)
    @Query("{'lastSeen': {'$gte': ?0}, 'occurrenceCount': {'$gte': ?1}}")
    List<ErrorPattern> findTrendingPatterns(LocalDateTime since, int minOccurrences);
    
    // Find related patterns
    @Query("{'relatedPatterns': {'$in': [?0]}}")
    List<ErrorPattern> findRelatedPatterns(String patternId);
    
    // Find patterns for ML training (validated with high confidence)
    @Query("{'validated': true, 'confidenceScore': {'$gte': 0.8}}")
    List<ErrorPattern> findPatternsForMLTraining();
    
    // Find patterns by occurrence count range
    @Query("{'occurrenceCount': {'$gte': ?0, '$lte': ?1}}")
    List<ErrorPattern> findByOccurrenceCountBetween(int minCount, int maxCount);
    
    // Find patterns updated after timestamp
    @Query("{'updatedAt': {'$gte': ?0}}")
    List<ErrorPattern> findByUpdatedAtAfter(LocalDateTime timestamp);
    
    // Complex query for pattern similarity analysis
    @Query("{'serviceName': ?0, 'errorType': ?1, 'className': ?2, 'signature': {'$ne': ?3}}")
    List<ErrorPattern> findSimilarPatterns(String serviceName, String errorType, String className, String excludeSignature);
    
    // Find fixable patterns (has automated fix and high confidence)
    @Query("{'hasAutomatedFix': true, 'confidenceScore': {'$gte': ?0}, 'validated': true}")
    List<ErrorPattern> findFixablePatterns(double minConfidence);
    
    // Statistical queries for analytics
    
    // Get pattern distribution by severity
    @Query(value = "{}", fields = "{'severity': 1}")
    List<ErrorPattern> findAllSeverities();
    
    // Get most frequent patterns
    List<ErrorPattern> findTop10ByOrderByOccurrenceCountDesc();
    
    // Get recent critical patterns
    @Query("{'severity': 'critical', 'lastSeen': {'$gte': ?0}}")
    List<ErrorPattern> findRecentCriticalPatterns(LocalDateTime since);
}