package com.ecommerce.monitoring.service;

import com.ecommerce.monitoring.entity.MonitoringEvent;
import com.ecommerce.monitoring.repository.MonitoringEventRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MonitoringEventService {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringEventService.class);
    
    private final MonitoringEventRepository repository;
    private final ErrorPatternAnalysisService patternAnalysisService;
    
    public MonitoringEventService(MonitoringEventRepository repository, 
                                ErrorPatternAnalysisService patternAnalysisService) {
        this.repository = repository;
        this.patternAnalysisService = patternAnalysisService;
    }
    
    /**
     * Save monitoring event and trigger pattern analysis
     */
    public MonitoringEvent saveEvent(MonitoringEvent event) {
        try {
            MonitoringEvent savedEvent = repository.save(event);
            
            // Trigger pattern analysis for error events
            if ("error".equals(event.getEventType()) && event.getStackTrace() != null) {
                patternAnalysisService.analyzeErrorPattern(savedEvent);
            }
            
            logger.debug("Saved monitoring event: {} from {} for service {}", 
                        event.getEventType(), event.getSource(), event.getServiceName());
            
            return savedEvent;
            
        } catch (Exception e) {
            logger.error("Error saving monitoring event", e);
            throw e;
        }
    }
    
    /**
     * Save multiple events in batch
     */
    public List<MonitoringEvent> saveEvents(List<MonitoringEvent> events) {
        try {
            List<MonitoringEvent> savedEvents = repository.saveAll(events);
            
            // Trigger pattern analysis for error events
            savedEvents.stream()
                      .filter(event -> "error".equals(event.getEventType()) && event.getStackTrace() != null)
                      .forEach(patternAnalysisService::analyzeErrorPattern);
            
            logger.info("Saved {} monitoring events", savedEvents.size());
            return savedEvents;
            
        } catch (Exception e) {
            logger.error("Error saving monitoring events batch", e);
            throw e;
        }
    }
    
    /**
     * Find events by service and time range
     */
    public List<MonitoringEvent> findEventsByServiceAndTimeRange(String serviceName, 
                                                               LocalDateTime startTime, 
                                                               LocalDateTime endTime) {
        return repository.findByServiceNameAndTimestampBetween(serviceName, startTime, endTime);
    }
    
    /**
     * Find events by correlation ID
     */
    public List<MonitoringEvent> findEventsByCorrelationId(String correlationId) {
        return repository.findByCorrelationId(correlationId);
    }
    
    /**
     * Find events by trace ID
     */
    public List<MonitoringEvent> findEventsByTraceId(String traceId) {
        return repository.findByTraceId(traceId);
    }
    
    /**
     * Find recent error events for a service
     */
    public List<MonitoringEvent> findRecentErrorsByService(String serviceName, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return repository.findByServiceNameAndEventTypeAndTimestampAfter(serviceName, "error", since);
    }
    
    /**
     * Find events by severity
     */
    public List<MonitoringEvent> findEventsBySeverity(String severity, LocalDateTime since) {
        return repository.findBySeverityAndTimestampAfter(severity, since);
    }
    
    /**
     * Find events by error signature
     */
    public List<MonitoringEvent> findEventsByErrorSignature(String errorSignature) {
        return repository.findByErrorSignature(errorSignature);
    }
    
    /**
     * Get event statistics by service
     */
    public Map<String, Long> getEventStatsByService(String serviceName, LocalDateTime since) {
        List<MonitoringEvent> events = repository.findByServiceNameAndTimestampAfter(serviceName, since);
        
        return events.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        MonitoringEvent::getEventType,
                        java.util.stream.Collectors.counting()
                    ));
    }
    
    /**
     * Get error rate for a service
     */
    public double getErrorRate(String serviceName, LocalDateTime since) {
        List<MonitoringEvent> allEvents = repository.findByServiceNameAndTimestampAfter(serviceName, since);
        long totalEvents = allEvents.size();
        
        if (totalEvents == 0) {
            return 0.0;
        }
        
        long errorEvents = allEvents.stream()
                                  .mapToLong(event -> "error".equals(event.getEventType()) ? 1 : 0)
                                  .sum();
        
        return (double) errorEvents / totalEvents * 100.0;
    }
    
    /**
     * Find events requiring attention (high severity, not auto-fixed)
     */
    public List<MonitoringEvent> findEventsRequiringAttention() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<MonitoringEvent> criticalEvents = repository.findBySeverityAndTimestampAfter("critical", since);
        List<MonitoringEvent> highEvents = repository.findBySeverityAndTimestampAfter("high", since);
        
        criticalEvents.addAll(highEvents);
        
        return criticalEvents.stream()
                           .filter(event -> !event.isAutoFixed())
                           .toList();
    }
    
    /**
     * Correlate events across different sources
     */
    public List<MonitoringEvent> findCorrelatedEvents(MonitoringEvent sourceEvent) {
        LocalDateTime startTime = sourceEvent.getTimestamp().minusMinutes(5);
        LocalDateTime endTime = sourceEvent.getTimestamp().plusMinutes(5);
        
        List<MonitoringEvent> timeRangeEvents = repository.findByTimestampBetween(startTime, endTime);
        
        return timeRangeEvents.stream()
                            .filter(event -> !event.getId().equals(sourceEvent.getId()))
                            .filter(event -> isCorrelated(sourceEvent, event))
                            .toList();
    }
    
    /**
     * Update event with correlation information
     */
    public MonitoringEvent updateEventCorrelation(String eventId, String correlationId, String traceId) {
        Optional<MonitoringEvent> eventOpt = repository.findById(eventId);
        
        if (eventOpt.isPresent()) {
            MonitoringEvent event = eventOpt.get();
            event.setCorrelationId(correlationId);
            event.setTraceId(traceId);
            return repository.save(event);
        }
        
        return null;
    }
    
    /**
     * Mark event as auto-fixed
     */
    public MonitoringEvent markEventAsAutoFixed(String eventId, String fixCommitId, String resolution) {
        Optional<MonitoringEvent> eventOpt = repository.findById(eventId);
        
        if (eventOpt.isPresent()) {
            MonitoringEvent event = eventOpt.get();
            event.setAutoFixed(true);
            event.setFixCommitId(fixCommitId);
            event.setResolution(resolution);
            return repository.save(event);
        }
        
        return null;
    }
    
    /**
     * Get health summary for all services
     */
    public Map<String, Object> getHealthSummary() {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        List<MonitoringEvent> recentEvents = repository.findByTimestampAfter(since);
        
        Map<String, Object> summary = new java.util.HashMap<>();
        
        // Group by service
        Map<String, List<MonitoringEvent>> eventsByService = recentEvents.stream()
                .collect(java.util.stream.Collectors.groupingBy(MonitoringEvent::getServiceName));
        
        for (Map.Entry<String, List<MonitoringEvent>> entry : eventsByService.entrySet()) {
            String serviceName = entry.getKey();
            List<MonitoringEvent> serviceEvents = entry.getValue();
            
            Map<String, Object> serviceHealth = new java.util.HashMap<>();
            serviceHealth.put("totalEvents", serviceEvents.size());
            serviceHealth.put("errorCount", serviceEvents.stream()
                                                       .mapToLong(e -> "error".equals(e.getEventType()) ? 1 : 0)
                                                       .sum());
            serviceHealth.put("criticalCount", serviceEvents.stream()
                                                          .mapToLong(e -> "critical".equals(e.getSeverity()) ? 1 : 0)
                                                          .sum());
            serviceHealth.put("autoFixedCount", serviceEvents.stream()
                                                           .mapToLong(e -> e.isAutoFixed() ? 1 : 0)
                                                           .sum());
            
            summary.put(serviceName, serviceHealth);
        }
        
        return summary;
    }
    
    // Private helper methods
    
    private boolean isCorrelated(MonitoringEvent event1, MonitoringEvent event2) {
        // Events are correlated if they:
        // 1. Are from the same service
        // 2. Have the same correlation ID
        // 3. Have the same trace ID
        // 4. Are related by error signature pattern
        
        if (event1.getServiceName().equals(event2.getServiceName())) {
            return true;
        }
        
        if (event1.getCorrelationId() != null && 
            event1.getCorrelationId().equals(event2.getCorrelationId())) {
            return true;
        }
        
        if (event1.getTraceId() != null && 
            event1.getTraceId().equals(event2.getTraceId())) {
            return true;
        }
        
        if (event1.getErrorSignature() != null && 
            event1.getErrorSignature().equals(event2.getErrorSignature())) {
            return true;
        }
        
        return false;
    }
}