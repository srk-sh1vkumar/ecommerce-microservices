package com.ecommerce.monitoring.repository;

import com.ecommerce.monitoring.entity.MonitoringEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonitoringEventRepository extends MongoRepository<MonitoringEvent, String> {
    
    // Find by service and time range
    List<MonitoringEvent> findByServiceNameAndTimestampBetween(String serviceName, 
                                                             LocalDateTime startTime, 
                                                             LocalDateTime endTime);
    
    // Find by service and after timestamp
    List<MonitoringEvent> findByServiceNameAndTimestampAfter(String serviceName, LocalDateTime timestamp);
    
    // Find by correlation ID
    List<MonitoringEvent> findByCorrelationId(String correlationId);
    
    // Find by trace ID
    List<MonitoringEvent> findByTraceId(String traceId);
    
    // Find by event type and time range
    List<MonitoringEvent> findByEventTypeAndTimestampBetween(String eventType, 
                                                           LocalDateTime startTime, 
                                                           LocalDateTime endTime);
    
    // Find by service, event type and after timestamp
    List<MonitoringEvent> findByServiceNameAndEventTypeAndTimestampAfter(String serviceName, 
                                                                        String eventType, 
                                                                        LocalDateTime timestamp);
    
    // Find by severity and after timestamp
    List<MonitoringEvent> findBySeverityAndTimestampAfter(String severity, LocalDateTime timestamp);
    
    // Find by source and after timestamp
    List<MonitoringEvent> findBySourceAndTimestampAfter(String source, LocalDateTime timestamp);
    
    // Find by error signature
    List<MonitoringEvent> findByErrorSignature(String errorSignature);
    
    // Find by time range
    List<MonitoringEvent> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    // Find after timestamp
    List<MonitoringEvent> findByTimestampAfter(LocalDateTime timestamp);
    
    // Find auto-fixed events
    List<MonitoringEvent> findByAutoFixedTrue();
    
    // Find events that need attention (not auto-fixed and high/critical severity)
    @Query("{'autoFixed': false, 'severity': {'$in': ['critical', 'high']}, 'timestamp': {'$gte': ?0}}")
    List<MonitoringEvent> findEventsNeedingAttention(LocalDateTime since);
    
    // Count events by service and type
    @Query(value = "{'serviceName': ?0, 'eventType': ?1, 'timestamp': {'$gte': ?2}}", count = true)
    long countByServiceNameAndEventTypeAndTimestampAfter(String serviceName, String eventType, LocalDateTime timestamp);
    
    // Count events by severity
    @Query(value = "{'severity': ?0, 'timestamp': {'$gte': ?1}}", count = true)
    long countBySeverityAndTimestampAfter(String severity, LocalDateTime timestamp);
    
    // Find events with stack trace (for error analysis)
    @Query("{'stackTrace': {'$exists': true, '$ne': null}, 'timestamp': {'$gte': ?0}}")
    List<MonitoringEvent> findEventsWithStackTrace(LocalDateTime since);
    
    // Find events by business context
    @Query("{'businessContext.?0': ?1, 'timestamp': {'$gte': ?2}}")
    List<MonitoringEvent> findByBusinessContext(String contextKey, Object contextValue, LocalDateTime since);
    
    // Find recent errors by service for pattern analysis
    @Query("{'serviceName': ?0, 'eventType': 'error', 'timestamp': {'$gte': ?1}}")
    List<MonitoringEvent> findRecentErrorsByService(String serviceName, LocalDateTime since);
    
    // Aggregation queries for analytics
    
    // Get error rate by service
    @Query(value = "{'serviceName': ?0, 'timestamp': {'$gte': ?1}}", count = true)
    long countTotalEventsByServiceSince(String serviceName, LocalDateTime since);
    
    // Find correlated events by multiple criteria
    @Query("{'$or': [" +
           "{'correlationId': ?0}, " +
           "{'traceId': ?1}, " +
           "{'serviceName': ?2}" +
           "], 'timestamp': {'$gte': ?3, '$lte': ?4}}")
    List<MonitoringEvent> findCorrelatedEvents(String correlationId, String traceId, String serviceName, 
                                             LocalDateTime startTime, LocalDateTime endTime);
    
    // Performance metrics queries
    @Query("{'eventType': 'performance', 'serviceName': ?0, 'timestamp': {'$gte': ?1}}")
    List<MonitoringEvent> findPerformanceEventsByService(String serviceName, LocalDateTime since);
    
    // Business transaction queries
    @Query("{'source': 'appdynamics', 'eventType': 'performance', 'metrics.transactionName': {'$exists': true}, 'timestamp': {'$gte': ?0}}")
    List<MonitoringEvent> findBusinessTransactionEvents(LocalDateTime since);
    
    // Error pattern queries for ML analysis
    @Query("{'errorSignature': {'$exists': true, '$ne': null}, 'serviceName': ?0, 'timestamp': {'$gte': ?1}}")
    List<MonitoringEvent> findErrorPatternsForService(String serviceName, LocalDateTime since);
    
    // Find events by multiple tags
    @Query("{'tags.?0': ?1, 'tags.?2': ?3, 'timestamp': {'$gte': ?4}}")
    List<MonitoringEvent> findByTags(String tag1Key, String tag1Value, String tag2Key, String tag2Value, LocalDateTime since);
}