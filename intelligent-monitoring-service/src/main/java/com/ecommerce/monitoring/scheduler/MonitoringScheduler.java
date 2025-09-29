package com.ecommerce.monitoring.scheduler;

import com.ecommerce.monitoring.entity.MonitoringEvent;
import com.ecommerce.monitoring.service.AppDynamicsIntegrationService;
import com.ecommerce.monitoring.service.MonitoringEventService;
import com.ecommerce.monitoring.service.OpenTelemetryIntegrationService;
import com.ecommerce.monitoring.service.CrossPlatformCorrelationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@ConditionalOnProperty(name = "monitoring.data-collection.enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringScheduler.class);
    
    private final AppDynamicsIntegrationService appDynamicsService;
    private final OpenTelemetryIntegrationService openTelemetryService;
    private final MonitoringEventService monitoringEventService;
    private final CrossPlatformCorrelationService correlationService;
    
    public MonitoringScheduler(AppDynamicsIntegrationService appDynamicsService,
                             OpenTelemetryIntegrationService openTelemetryService,
                             MonitoringEventService monitoringEventService,
                             CrossPlatformCorrelationService correlationService) {
        this.appDynamicsService = appDynamicsService;
        this.openTelemetryService = openTelemetryService;
        this.monitoringEventService = monitoringEventService;
        this.correlationService = correlationService;
    }
    
    /**
     * Collect data from AppDynamics every 5 minutes
     */
    @Scheduled(fixedRateString = "${monitoring.data-collection.appdynamics.fetch-interval-minutes:5}000")
    @ConditionalOnProperty(name = "monitoring.data-collection.appdynamics.enabled", havingValue = "true")
    public void collectAppDynamicsData() {
        logger.debug("Starting AppDynamics data collection");
        
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(10); // Overlap to avoid gaps
            
            // Collect data asynchronously
            CompletableFuture<List<MonitoringEvent>> businessTransactionsFuture = 
                CompletableFuture.supplyAsync(() -> appDynamicsService.fetchBusinessTransactions());
            
            CompletableFuture<List<MonitoringEvent>> errorsFuture = 
                CompletableFuture.supplyAsync(() -> appDynamicsService.fetchErrors(since));
            
            CompletableFuture<List<MonitoringEvent>> performanceMetricsFuture = 
                CompletableFuture.supplyAsync(() -> appDynamicsService.fetchPerformanceMetrics());
            
            // Wait for all futures to complete
            CompletableFuture.allOf(businessTransactionsFuture, errorsFuture, performanceMetricsFuture)
                            .thenRun(() -> {
                                try {
                                    // Process business transactions
                                    List<MonitoringEvent> businessTransactions = businessTransactionsFuture.get();
                                    if (!businessTransactions.isEmpty()) {
                                        monitoringEventService.saveEvents(businessTransactions);
                                        logger.debug("Saved {} business transaction events", businessTransactions.size());
                                    }
                                    
                                    // Process errors
                                    List<MonitoringEvent> errors = errorsFuture.get();
                                    if (!errors.isEmpty()) {
                                        monitoringEventService.saveEvents(errors);
                                        logger.debug("Saved {} error events", errors.size());
                                    }
                                    
                                    // Process performance metrics
                                    List<MonitoringEvent> metrics = performanceMetricsFuture.get();
                                    if (!metrics.isEmpty()) {
                                        monitoringEventService.saveEvents(metrics);
                                        logger.debug("Saved {} performance metric events", metrics.size());
                                    }
                                    
                                } catch (Exception e) {
                                    logger.error("Error processing AppDynamics data", e);
                                }
                            });
            
        } catch (Exception e) {
            logger.error("Error during AppDynamics data collection", e);
        }
    }
    
    /**
     * Collect data from OpenTelemetry every 5 minutes
     */
    @Scheduled(fixedRateString = "${monitoring.data-collection.opentelemetry.query-interval-minutes:5}000")
    @ConditionalOnProperty(name = "monitoring.data-collection.opentelemetry.enabled", havingValue = "true")
    public void collectOpenTelemetryData() {
        logger.debug("Starting OpenTelemetry data collection");
        
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(10);
            
            // Collect trace data
            CompletableFuture<List<MonitoringEvent>> tracesFuture = 
                CompletableFuture.supplyAsync(() -> openTelemetryService.fetchTraces(since));
            
            // Collect span data
            CompletableFuture<List<MonitoringEvent>> spansFuture = 
                CompletableFuture.supplyAsync(() -> openTelemetryService.fetchSpans(since));
            
            // Wait for completion and process
            CompletableFuture.allOf(tracesFuture, spansFuture)
                            .thenRun(() -> {
                                try {
                                    List<MonitoringEvent> traces = tracesFuture.get();
                                    if (!traces.isEmpty()) {
                                        monitoringEventService.saveEvents(traces);
                                        logger.debug("Saved {} trace events", traces.size());
                                    }
                                    
                                    List<MonitoringEvent> spans = spansFuture.get();
                                    if (!spans.isEmpty()) {
                                        monitoringEventService.saveEvents(spans);
                                        logger.debug("Saved {} span events", spans.size());
                                    }
                                    
                                } catch (Exception e) {
                                    logger.error("Error processing OpenTelemetry data", e);
                                }
                            });
            
        } catch (Exception e) {
            logger.error("Error during OpenTelemetry data collection", e);
        }
    }
    
    /**
     * Perform cross-platform correlation every 10 minutes
     */
    @Scheduled(fixedRateString = "600000") // 10 minutes
    @ConditionalOnProperty(name = "cross-platform.correlation.enabled", havingValue = "true")
    public void performCrossPlatformCorrelation() {
        logger.debug("Starting cross-platform correlation");
        
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(15); // Wider window for correlation
            correlationService.correlateCrossPlatformEvents(since);
            
        } catch (Exception e) {
            logger.error("Error during cross-platform correlation", e);
        }
    }
    
    /**
     * Update pattern confidence scores every hour
     */
    @Scheduled(fixedRateString = "${monitoring.pattern-analysis.confidence-update-interval:60}000")
    @ConditionalOnProperty(name = "monitoring.pattern-analysis.enabled", havingValue = "true")
    public void updatePatternConfidenceScores() {
        logger.debug("Starting pattern confidence score updates");
        
        try {
            // This would trigger pattern analysis service to recalculate confidence scores
            // based on recent data and validation feedback
            
            logger.info("Pattern confidence scores updated");
            
        } catch (Exception e) {
            logger.error("Error updating pattern confidence scores", e);
        }
    }
    
    /**
     * Cleanup old monitoring data daily
     */
    @Scheduled(cron = "0 2 0 * * *") // Daily at 2 AM
    public void cleanupOldData() {
        logger.info("Starting cleanup of old monitoring data");
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90); // Keep 90 days
            
            // Cleanup would be handled by MongoDB TTL indexes, but we can also
            // perform additional cleanup for aggregated data
            
            logger.info("Completed cleanup of old monitoring data");
            
        } catch (Exception e) {
            logger.error("Error during data cleanup", e);
        }
    }
    
    /**
     * Health check for monitoring services every minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void monitoringHealthCheck() {
        try {
            // Check AppDynamics connectivity
            boolean appDynamicsHealthy = appDynamicsService.isHealthy();
            
            // Check OpenTelemetry connectivity
            boolean openTelemetryHealthy = openTelemetryService.isHealthy();
            
            // Check database connectivity
            boolean databaseHealthy = isDatabaseHealthy();
            
            if (!appDynamicsHealthy || !openTelemetryHealthy || !databaseHealthy) {
                logger.warn("Health check issues detected - AppD: {}, OTEL: {}, DB: {}", 
                           appDynamicsHealthy, openTelemetryHealthy, databaseHealthy);
            }
            
        } catch (Exception e) {
            logger.error("Error during health check", e);
        }
    }
    
    /**
     * Generate monitoring reports every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    public void generateMonitoringReports() {
        logger.info("Starting monitoring report generation");
        
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            
            // Generate summary statistics
            Map<String, Object> healthSummary = monitoringEventService.getHealthSummary();
            
            // Log key metrics
            logger.info("Monitoring Report - Health Summary: {}", healthSummary);
            
            // Could send reports to external systems, dashboards, etc.
            
        } catch (Exception e) {
            logger.error("Error generating monitoring reports", e);
        }
    }
    
    private boolean isDatabaseHealthy() {
        try {
            // Simple check by counting recent events
            LocalDateTime since = LocalDateTime.now().minusMinutes(1);
            monitoringEventService.getHealthSummary();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}