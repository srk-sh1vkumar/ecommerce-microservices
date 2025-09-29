package com.ecommerce.monitoring.repository;

import com.ecommerce.monitoring.entity.AuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for audit event persistence and querying
 */
@Repository
public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {
    
    /**
     * Find audit events by event type
     */
    List<AuditEvent> findByEventTypeOrderByTimestampDesc(String eventType);
    
    /**
     * Find audit events by user ID
     */
    List<AuditEvent> findByUserIdOrderByTimestampDesc(String userId);
    
    /**
     * Find audit events by date range
     */
    List<AuditEvent> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find audit events by category and timestamp after
     */
    List<AuditEvent> findByCategoryAndTimestampAfterOrderByTimestampDesc(String category, LocalDateTime timestamp);
    
    /**
     * Find audit events by severity
     */
    List<AuditEvent> findBySeverityOrderByTimestampDesc(String severity);
    
    /**
     * Find audit events by event type containing and timestamp after
     */
    List<AuditEvent> findByEventTypeContainingAndTimestampAfterOrderByTimestampDesc(String eventType, LocalDateTime timestamp);
    
    /**
     * Count events by event type and timestamp after
     */
    long countByEventTypeAndTimestampAfter(String eventType, LocalDateTime timestamp);
    
    /**
     * Count events by category and timestamp after
     */
    long countByCategoryAndTimestampAfter(String category, LocalDateTime timestamp);
    
    /**
     * Find audit events by source
     */
    List<AuditEvent> findBySourceOrderByTimestampDesc(String source);
    
    /**
     * Find audit events by user and category
     */
    List<AuditEvent> findByUserIdAndCategoryOrderByTimestampDesc(String userId, String category);
    
    /**
     * Find recent critical events
     */
    List<AuditEvent> findBySeverityAndTimestampAfterOrderByTimestampDesc(String severity, LocalDateTime timestamp);
    
    /**
     * Find events by timestamp after (for cleanup operations)
     */
    List<AuditEvent> findByTimestampBefore(LocalDateTime timestamp);
    
    /**
     * Delete events older than specified timestamp (for data retention)
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);
}