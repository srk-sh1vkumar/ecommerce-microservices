package com.ecommerce.monitoring.controller;

import com.ecommerce.monitoring.entity.MonitoringEvent;
import com.ecommerce.monitoring.entity.ErrorPattern;
import com.ecommerce.monitoring.entity.AutomatedFix;
import com.ecommerce.monitoring.service.MonitoringEventService;
import com.ecommerce.monitoring.service.ErrorPatternAnalysisService;
import com.ecommerce.monitoring.service.AutomatedFixingService;
import com.ecommerce.monitoring.repository.AutomatedFixRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);
    
    private final MonitoringEventService monitoringEventService;
    private final ErrorPatternAnalysisService errorPatternAnalysisService;
    private final AutomatedFixingService automatedFixingService;
    private final AutomatedFixRepository automatedFixRepository;
    
    public MonitoringController(MonitoringEventService monitoringEventService,
                              ErrorPatternAnalysisService errorPatternAnalysisService,
                              AutomatedFixingService automatedFixingService,
                              AutomatedFixRepository automatedFixRepository) {
        this.monitoringEventService = monitoringEventService;
        this.errorPatternAnalysisService = errorPatternAnalysisService;
        this.automatedFixingService = automatedFixingService;
        this.automatedFixRepository = automatedFixRepository;
    }
    
    // === Monitoring Events API ===
    
    @PostMapping("/events")
    public ResponseEntity<MonitoringEvent> createEvent(@RequestBody MonitoringEvent event) {
        try {
            MonitoringEvent savedEvent = monitoringEventService.saveEvent(event);
            return ResponseEntity.ok(savedEvent);
        } catch (Exception e) {
            logger.error("Error creating monitoring event", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/events/batch")
    public ResponseEntity<List<MonitoringEvent>> createEvents(@RequestBody List<MonitoringEvent> events) {
        try {
            List<MonitoringEvent> savedEvents = monitoringEventService.saveEvents(events);
            return ResponseEntity.ok(savedEvents);
        } catch (Exception e) {
            logger.error("Error creating monitoring events batch", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/events/service/{serviceName}")
    public ResponseEntity<List<MonitoringEvent>> getEventsByService(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "24") int hours) {
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<MonitoringEvent> events = monitoringEventService.findEventsByServiceAndTimeRange(
            serviceName, since, LocalDateTime.now());
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/correlation/{correlationId}")
    public ResponseEntity<List<MonitoringEvent>> getEventsByCorrelation(@PathVariable String correlationId) {
        List<MonitoringEvent> events = monitoringEventService.findEventsByCorrelationId(correlationId);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/trace/{traceId}")
    public ResponseEntity<List<MonitoringEvent>> getEventsByTrace(@PathVariable String traceId) {
        List<MonitoringEvent> events = monitoringEventService.findEventsByTraceId(traceId);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/errors/{serviceName}")
    public ResponseEntity<List<MonitoringEvent>> getRecentErrors(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "24") int hours) {
        
        List<MonitoringEvent> events = monitoringEventService.findRecentErrorsByService(serviceName, hours);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/severity/{severity}")
    public ResponseEntity<List<MonitoringEvent>> getEventsBySeverity(
            @PathVariable String severity,
            @RequestParam(defaultValue = "24") int hours) {
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<MonitoringEvent> events = monitoringEventService.findEventsBySeverity(severity, since);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/attention")
    public ResponseEntity<List<MonitoringEvent>> getEventsRequiringAttention() {
        List<MonitoringEvent> events = monitoringEventService.findEventsRequiringAttention();
        return ResponseEntity.ok(events);
    }
    
    @PutMapping("/events/{eventId}/correlation")
    public ResponseEntity<MonitoringEvent> updateEventCorrelation(
            @PathVariable String eventId,
            @RequestParam String correlationId,
            @RequestParam(required = false) String traceId) {
        
        MonitoringEvent updatedEvent = monitoringEventService.updateEventCorrelation(eventId, correlationId, traceId);
        return updatedEvent != null ? ResponseEntity.ok(updatedEvent) : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/events/{eventId}/auto-fixed")
    public ResponseEntity<MonitoringEvent> markEventAsAutoFixed(
            @PathVariable String eventId,
            @RequestParam String fixCommitId,
            @RequestParam String resolution) {
        
        MonitoringEvent updatedEvent = monitoringEventService.markEventAsAutoFixed(eventId, fixCommitId, resolution);
        return updatedEvent != null ? ResponseEntity.ok(updatedEvent) : ResponseEntity.notFound().build();
    }
    
    // === Error Patterns API ===
    
    @GetMapping("/patterns")
    public ResponseEntity<List<ErrorPattern>> getAllPatterns() {
        List<ErrorPattern> patterns = errorPatternAnalysisService.getPatternsRequiringAttention();
        return ResponseEntity.ok(patterns);
    }
    
    @GetMapping("/patterns/service/{serviceName}")
    public ResponseEntity<List<ErrorPattern>> getPatternsByService(@PathVariable String serviceName) {
        // Implementation would call repository directly
        return ResponseEntity.ok(List.of()); // Placeholder
    }
    
    @GetMapping("/patterns/fixable")
    public ResponseEntity<List<ErrorPattern>> getFixablePatterns() {
        List<ErrorPattern> patterns = errorPatternAnalysisService.getFixablePatterns();
        return ResponseEntity.ok(patterns);
    }
    
    @GetMapping("/patterns/{patternId}/similar")
    public ResponseEntity<List<ErrorPattern>> getSimilarPatterns(@PathVariable String patternId) {
        // Implementation would find pattern first then get similar ones
        return ResponseEntity.ok(List.of()); // Placeholder
    }
    
    @PutMapping("/patterns/{patternId}/validate")
    public ResponseEntity<Void> validatePattern(
            @PathVariable String patternId,
            @RequestParam boolean isValid) {
        
        errorPatternAnalysisService.validatePattern(patternId, isValid);
        return ResponseEntity.ok().build();
    }
    
    // === Automated Fixes API ===
    
    @GetMapping("/fixes")
    public ResponseEntity<List<AutomatedFix>> getAllFixes() {
        List<AutomatedFix> fixes = automatedFixRepository.findAll();
        return ResponseEntity.ok(fixes);
    }
    
    @GetMapping("/fixes/service/{serviceName}")
    public ResponseEntity<List<AutomatedFix>> getFixesByService(@PathVariable String serviceName) {
        List<AutomatedFix> fixes = automatedFixRepository.findByServiceName(serviceName);
        return ResponseEntity.ok(fixes);
    }
    
    @GetMapping("/fixes/status/{status}")
    public ResponseEntity<List<AutomatedFix>> getFixesByStatus(@PathVariable String status) {
        List<AutomatedFix> fixes = automatedFixRepository.findByStatus(status);
        return ResponseEntity.ok(fixes);
    }
    
    @GetMapping("/fixes/successful")
    public ResponseEntity<List<AutomatedFix>> getSuccessfulFixes() {
        List<AutomatedFix> fixes = automatedFixRepository.findSuccessfulFixes();
        return ResponseEntity.ok(fixes);
    }
    
    @GetMapping("/fixes/failed")
    public ResponseEntity<List<AutomatedFix>> getFailedFixes() {
        List<AutomatedFix> fixes = automatedFixRepository.findFailedFixes();
        return ResponseEntity.ok(fixes);
    }
    
    @GetMapping("/fixes/attention")
    public ResponseEntity<List<AutomatedFix>> getFixesNeedingAttention() {
        List<AutomatedFix> fixes = automatedFixRepository.findFixesNeedingAttention();
        return ResponseEntity.ok(fixes);
    }
    
    @PostMapping("/fixes/{patternId}/trigger")
    public ResponseEntity<Void> triggerAutomatedFix(@PathVariable String patternId) {
        try {
            // Implementation would find pattern first then trigger fix
            logger.info("Manual trigger for automated fix requested for pattern: {}", patternId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error triggering automated fix", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // === Analytics and Health API ===
    
    @GetMapping("/health/summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        Map<String, Object> summary = monitoringEventService.getHealthSummary();
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/analytics/error-rate/{serviceName}")
    public ResponseEntity<Map<String, Object>> getErrorRate(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "24") int hours) {
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        double errorRate = monitoringEventService.getErrorRate(serviceName, since);
        Map<String, Long> statsLong = monitoringEventService.getEventStatsByService(serviceName, since);
        Map<String, Object> stats = new HashMap<>(statsLong);
        
        Map<String, Object> result = Map.of(
            "errorRate", errorRate,
            "statistics", stats,
            "serviceName", serviceName,
            "timeRangeHours", hours
        );
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/analytics/fix-summary")
    public ResponseEntity<Map<String, Object>> getFixSummary() {
        LocalDateTime since = LocalDateTime.now().minusDays(7); // Last 7 days
        
        Map<String, Object> summary = Map.of(
            "totalFixes", automatedFixRepository.count(),
            "successfulFixes", automatedFixRepository.findSuccessfulFixes().size(),
            "failedFixes", automatedFixRepository.findFailedFixes().size(),
            "recentSuccessful", automatedFixRepository.findRecentSuccessfulFixes(since).size(),
            "fixesNeedingAttention", automatedFixRepository.findFixesNeedingAttention().size()
        );
        
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMonitoringStatus() {
        Map<String, Object> status = Map.of(
            "service", "intelligent-monitoring-service",
            "status", "operational",
            "timestamp", LocalDateTime.now(),
            "features", Map.of(
                "appDynamicsIntegration", true,
                "openTelemetryIntegration", true,
                "automatedFixing", true,
                "patternAnalysis", true,
                "crossPlatformCorrelation", true
            )
        );
        
        return ResponseEntity.ok(status);
    }
}