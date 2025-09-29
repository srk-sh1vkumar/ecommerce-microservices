package com.ecommerce.monitoring.service;

import com.ecommerce.monitoring.entity.MonitoringEvent;
import com.ecommerce.monitoring.repository.MonitoringEventRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CrossPlatformCorrelationService {
    
    private static final Logger logger = LoggerFactory.getLogger(CrossPlatformCorrelationService.class);
    
    private final MonitoringEventRepository monitoringEventRepository;
    private final MonitoringEventService monitoringEventService;
    private final AppDynamicsIntegrationService appDynamicsService;
    private final OpenTelemetryIntegrationService openTelemetryService;
    
    // Correlation weights for different sources
    private static final Map<String, Double> SOURCE_WEIGHTS = Map.of(
        "appdynamics", 0.4,
        "opentelemetry", 0.3,
        "frontend", 0.2,
        "loadtest", 0.1
    );
    
    public CrossPlatformCorrelationService(MonitoringEventRepository monitoringEventRepository,
                                         MonitoringEventService monitoringEventService,
                                         AppDynamicsIntegrationService appDynamicsService,
                                         OpenTelemetryIntegrationService openTelemetryService) {
        this.monitoringEventRepository = monitoringEventRepository;
        this.monitoringEventService = monitoringEventService;
        this.appDynamicsService = appDynamicsService;
        this.openTelemetryService = openTelemetryService;
    }
    
    /**
     * Correlate events across different platforms
     */
    public void correlateCrossPlatformEvents(LocalDateTime since) {
        try {
            logger.info("Starting cross-platform correlation since: {}", since);
            
            // Get all events from the time window
            List<MonitoringEvent> allEvents = monitoringEventRepository.findByTimestampAfter(since);
            
            // Group events by potential correlation keys
            Map<String, List<MonitoringEvent>> correlationGroups = groupEventsByCorrelation(allEvents);
            
            // Process each correlation group
            for (Map.Entry<String, List<MonitoringEvent>> entry : correlationGroups.entrySet()) {
                String correlationKey = entry.getKey();
                List<MonitoringEvent> correlatedEvents = entry.getValue();
                
                if (correlatedEvents.size() > 1) { // Only correlate if multiple events
                    processCorrelationGroup(correlationKey, correlatedEvents);
                }
            }
            
            logger.info("Completed cross-platform correlation. Processed {} correlation groups", 
                       correlationGroups.size());
            
        } catch (Exception e) {
            logger.error("Error during cross-platform correlation", e);
        }
    }
    
    /**
     * Group events by potential correlation identifiers
     */
    private Map<String, List<MonitoringEvent>> groupEventsByCorrelation(List<MonitoringEvent> events) {
        Map<String, List<MonitoringEvent>> groups = new HashMap<>();
        
        for (MonitoringEvent event : events) {
            Set<String> correlationKeys = extractCorrelationKeys(event);
            
            for (String key : correlationKeys) {
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
            }
        }
        
        // Filter out groups with only single events
        return groups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Extract correlation keys from an event
     */
    private Set<String> extractCorrelationKeys(MonitoringEvent event) {
        Set<String> keys = new HashSet<>();
        
        // Use existing correlation ID if present
        if (event.getCorrelationId() != null) {
            keys.add("correlation:" + event.getCorrelationId());
        }
        
        // Use trace ID if present
        if (event.getTraceId() != null) {
            keys.add("trace:" + event.getTraceId());
        }
        
        // Use business context for correlation
        if (event.getBusinessContext() != null) {
            Map<String, Object> context = event.getBusinessContext();
            
            // Session-based correlation
            if (context.containsKey("sessionId")) {
                keys.add("session:" + context.get("sessionId"));
            }
            
            // User-based correlation
            if (context.containsKey("userEmail")) {
                keys.add("user:" + context.get("userEmail"));
            }
            
            // Transaction-based correlation
            if (context.containsKey("transactionId")) {
                keys.add("transaction:" + context.get("transactionId"));
            }
        }
        
        // Service + time window correlation (for events in same service within short time)
        if (event.getServiceName() != null) {
            long timeWindow = event.getTimestamp().getHour() * 60 + event.getTimestamp().getMinute() / 5; // 5-minute windows
            keys.add("service-time:" + event.getServiceName() + ":" + timeWindow);
        }
        
        // Error signature correlation
        if (event.getErrorSignature() != null) {
            keys.add("error:" + event.getErrorSignature());
        }
        
        return keys;
    }
    
    /**
     * Process a group of correlated events
     */
    private void processCorrelationGroup(String correlationKey, List<MonitoringEvent> events) {
        try {
            logger.debug("Processing correlation group: {} with {} events", correlationKey, events.size());
            
            // Generate unified correlation ID for the group
            String unifiedCorrelationId = generateUnifiedCorrelationId(correlationKey, events);
            
            // Calculate correlation confidence score
            double confidenceScore = calculateCorrelationConfidence(events);
            
            if (confidenceScore >= 0.7) { // Only process high-confidence correlations
                
                // Update all events with unified correlation
                for (MonitoringEvent event : events) {
                    if (event.getCorrelationId() == null || !event.getCorrelationId().equals(unifiedCorrelationId)) {
                        monitoringEventService.updateEventCorrelation(
                            event.getId(), 
                            unifiedCorrelationId, 
                            findBestTraceId(events)
                        );
                    }
                }
                
                // Create correlation summary
                MonitoringEvent correlationSummary = createCorrelationSummary(unifiedCorrelationId, events, confidenceScore);
                monitoringEventService.saveEvent(correlationSummary);
                
                // Send correlation data to external systems
                sendCorrelationToExternalSystems(unifiedCorrelationId, events, confidenceScore);
                
                logger.debug("Successfully correlated {} events with confidence: {}", 
                           events.size(), confidenceScore);
            } else {
                logger.debug("Skipping low-confidence correlation group: {} (confidence: {})", 
                           correlationKey, confidenceScore);
            }
            
        } catch (Exception e) {
            logger.error("Error processing correlation group: " + correlationKey, e);
        }
    }
    
    /**
     * Generate unified correlation ID for a group of events
     */
    private String generateUnifiedCorrelationId(String correlationKey, List<MonitoringEvent> events) {
        // Use existing correlation ID if all events have the same one
        Set<String> existingCorrelationIds = events.stream()
                .map(MonitoringEvent::getCorrelationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (existingCorrelationIds.size() == 1) {
            return existingCorrelationIds.iterator().next();
        }
        
        // Generate new unified correlation ID
        String timestamp = String.valueOf(System.currentTimeMillis());
        String hash = Integer.toHexString(correlationKey.hashCode());
        return "unified-" + hash + "-" + timestamp.substring(timestamp.length() - 6);
    }
    
    /**
     * Calculate confidence score for correlation
     */
    private double calculateCorrelationConfidence(List<MonitoringEvent> events) {
        double confidence = 0.0;
        int totalChecks = 0;
        
        // Check source diversity (better correlation if from different sources)
        Set<String> sources = events.stream()
                                   .map(MonitoringEvent::getSource)
                                   .collect(Collectors.toSet());
        confidence += Math.min(sources.size() * 0.2, 0.4); // Max 0.4 for source diversity
        totalChecks++;
        
        // Check time proximity
        if (events.size() > 1) {
            LocalDateTime earliest = events.stream()
                                          .map(MonitoringEvent::getTimestamp)
                                          .min(LocalDateTime::compareTo)
                                          .orElse(LocalDateTime.now());
            LocalDateTime latest = events.stream()
                                        .map(MonitoringEvent::getTimestamp)
                                        .max(LocalDateTime::compareTo)
                                        .orElse(LocalDateTime.now());
            
            long timeDiffMinutes = java.time.Duration.between(earliest, latest).toMinutes();
            double timeScore = Math.max(0.0, 1.0 - (timeDiffMinutes / 60.0)); // Lower score for larger time gaps
            confidence += timeScore * 0.3; // Max 0.3 for time proximity
            totalChecks++;
        }
        
        // Check business context overlap
        long eventsWithBusinessContext = events.stream()
                                              .mapToLong(e -> e.getBusinessContext() != null ? 1 : 0)
                                              .sum();
        if (eventsWithBusinessContext > 0) {
            confidence += (eventsWithBusinessContext / (double) events.size()) * 0.2; // Max 0.2 for business context
            totalChecks++;
        }
        
        // Check service relationship
        Set<String> services = events.stream()
                                    .map(MonitoringEvent::getServiceName)
                                    .collect(Collectors.toSet());
        if (services.size() > 1) {
            confidence += 0.1; // Bonus for cross-service correlation
        }
        totalChecks++;
        
        return Math.min(confidence, 1.0);
    }
    
    /**
     * Find the best trace ID from a group of events
     */
    private String findBestTraceId(List<MonitoringEvent> events) {
        // Prefer trace IDs from OpenTelemetry events
        return events.stream()
                    .filter(e -> "opentelemetry".equals(e.getSource()) && e.getTraceId() != null)
                    .map(MonitoringEvent::getTraceId)
                    .findFirst()
                    .orElse(events.stream()
                                 .map(MonitoringEvent::getTraceId)
                                 .filter(Objects::nonNull)
                                 .findFirst()
                                 .orElse(null));
    }
    
    /**
     * Create correlation summary event
     */
    private MonitoringEvent createCorrelationSummary(String correlationId, List<MonitoringEvent> events, double confidence) {
        MonitoringEvent summary = new MonitoringEvent("cross-platform-correlation", "correlation", "info", "correlation-service");
        
        summary.setCorrelationId(correlationId);
        summary.setDescription("Cross-platform correlation summary");
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("correlatedEventCount", events.size());
        metrics.put("confidenceScore", confidence);
        metrics.put("sources", events.stream().map(MonitoringEvent::getSource).distinct().collect(Collectors.toList()));
        metrics.put("services", events.stream().map(MonitoringEvent::getServiceName).distinct().collect(Collectors.toList()));
        metrics.put("timeSpanMinutes", calculateTimeSpan(events));
        
        summary.setMetrics(metrics);
        
        Map<String, String> tags = new HashMap<>();
        tags.put("correlationType", "cross-platform");
        tags.put("confidence", String.valueOf(confidence));
        
        summary.setTags(tags);
        
        return summary;
    }
    
    /**
     * Calculate time span of events in minutes
     */
    private long calculateTimeSpan(List<MonitoringEvent> events) {
        if (events.size() < 2) return 0;
        
        LocalDateTime earliest = events.stream()
                                      .map(MonitoringEvent::getTimestamp)
                                      .min(LocalDateTime::compareTo)
                                      .orElse(LocalDateTime.now());
        LocalDateTime latest = events.stream()
                                    .map(MonitoringEvent::getTimestamp)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(LocalDateTime.now());
        
        return java.time.Duration.between(earliest, latest).toMinutes();
    }
    
    /**
     * Send correlation data to external monitoring systems
     */
    private void sendCorrelationToExternalSystems(String correlationId, List<MonitoringEvent> events, double confidence) {
        try {
            Map<String, Object> correlationData = new HashMap<>();
            correlationData.put("correlationId", correlationId);
            correlationData.put("eventCount", events.size());
            correlationData.put("confidence", confidence);
            correlationData.put("sources", events.stream().map(MonitoringEvent::getSource).distinct().collect(Collectors.toList()));
            
            // Send to AppDynamics
            String traceId = findBestTraceId(events);
            if (traceId != null) {
                appDynamicsService.sendCorrelationData(traceId, correlationData);
            }
            
            // Send to OpenTelemetry
            String spanId = events.stream()
                                 .filter(e -> e.getSpanId() != null)
                                 .map(MonitoringEvent::getSpanId)
                                 .findFirst()
                                 .orElse(null);
            if (traceId != null && spanId != null) {
                openTelemetryService.sendCorrelationData(traceId, spanId, correlationData);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to send correlation data to external systems", e);
        }
    }
    
    /**
     * Get correlation statistics
     */
    public Map<String, Object> getCorrelationStatistics(LocalDateTime since) {
        List<MonitoringEvent> correlationEvents = monitoringEventRepository.findBySourceAndTimestampAfter("cross-platform-correlation", since);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCorrelations", correlationEvents.size());
        
        if (!correlationEvents.isEmpty()) {
            double avgConfidence = correlationEvents.stream()
                                                  .mapToDouble(e -> (Double) e.getMetrics().getOrDefault("confidenceScore", 0.0))
                                                  .average()
                                                  .orElse(0.0);
            stats.put("averageConfidence", avgConfidence);
            
            int totalCorrelatedEvents = correlationEvents.stream()
                                                        .mapToInt(e -> (Integer) e.getMetrics().getOrDefault("correlatedEventCount", 0))
                                                        .sum();
            stats.put("totalCorrelatedEvents", totalCorrelatedEvents);
        }
        
        return stats;
    }
}