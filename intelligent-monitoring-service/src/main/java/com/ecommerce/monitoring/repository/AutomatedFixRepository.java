package com.ecommerce.monitoring.repository;

import com.ecommerce.monitoring.entity.AutomatedFix;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AutomatedFixRepository extends MongoRepository<AutomatedFix, String> {
    
    // Find by error pattern ID
    List<AutomatedFix> findByErrorPatternId(String errorPatternId);
    
    // Find by service name
    List<AutomatedFix> findByServiceName(String serviceName);
    
    // Find by status
    List<AutomatedFix> findByStatus(String status);
    
    // Find by fix type
    List<AutomatedFix> findByFixType(String fixType);
    
    // Find successful fixes (validated and tests passed)
    @Query("{'status': 'validated', 'testsPassed': true}")
    List<AutomatedFix> findSuccessfulFixes();
    
    // Find failed fixes
    @Query("{'status': 'failed'}")
    List<AutomatedFix> findFailedFixes();
    
    // Find fixes needing attention (failed or requiring manual review)
    @Query("{'$or': [{'status': 'failed'}, {'requiresManualReview': true}]}")
    List<AutomatedFix> findFixesNeedingAttention();
    
    // Find recent fixes
    List<AutomatedFix> findByTimestampAfter(LocalDateTime timestamp);
    
    // Find fixes by service and status
    List<AutomatedFix> findByServiceNameAndStatus(String serviceName, String status);
    
    // Find fixes applied by system
    List<AutomatedFix> findByAppliedBy(String appliedBy);
    
    // Find fixes with performance impact
    @Query("{'performanceImpact': true}")
    List<AutomatedFix> findFixesWithPerformanceImpact();
    
    // Count fixes by status
    @Query(value = "{'status': ?0}", count = true)
    long countByStatus(String status);
    
    // Count fixes by service
    @Query(value = "{'serviceName': ?0}", count = true)
    long countByServiceName(String serviceName);
    
    // Find recent successful fixes for reporting
    @Query("{'status': 'validated', 'testsPassed': true, 'timestamp': {'$gte': ?0}}")
    List<AutomatedFix> findRecentSuccessfulFixes(LocalDateTime since);
    
    // Find fixes that need validation
    @Query("{'status': 'tested', 'testsPassed': true}")
    List<AutomatedFix> findFixesAwaitingValidation();
    
    // Find rolled back fixes
    @Query("{'status': 'rolled_back'}")
    List<AutomatedFix> findRolledBackFixes();
    
    // Statistical queries for analytics
    
    // Find fixes by time range
    List<AutomatedFix> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    // Find fixes by commit ID
    List<AutomatedFix> findByCommitId(String commitId);
    
    // Find fixes by branch name
    List<AutomatedFix> findByBranchName(String branchName);
    
    // Find fix by review ID
    Optional<AutomatedFix> findByReviewId(String reviewId);
}