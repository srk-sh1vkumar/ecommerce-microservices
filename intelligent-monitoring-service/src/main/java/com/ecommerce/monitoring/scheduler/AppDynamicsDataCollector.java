package com.ecommerce.monitoring.scheduler;

import com.ecommerce.monitoring.entity.MonitoringEvent;
import com.ecommerce.monitoring.service.AppDynamicsIntegrationService;
import com.ecommerce.monitoring.service.AppDynamicsAuthService;
import com.ecommerce.monitoring.service.MonitoringEventService;
import com.ecommerce.monitoring.service.ErrorPatternAnalysisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduled service to collect data from AppDynamics using OAuth2 authentication
 */
@Service
public class AppDynamicsDataCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(AppDynamicsDataCollector.class);
    
    @Value("${appdynamics.data-collection.enabled:true}")
    private boolean dataCollectionEnabled;
    
    @Value("${appdynamics.data-collection.parallel-execution:true}")
    private boolean parallelExecution;
    
    @Value("${appdynamics.data-collection.max-threads:5}")
    private int maxThreads;
    
    private final AppDynamicsIntegrationService appDynamicsService;
    private final AppDynamicsAuthService authService;
    private final MonitoringEventService monitoringEventService;
    private final ErrorPatternAnalysisService errorPatternService;
    private final ExecutorService executorService;
    
    public AppDynamicsDataCollector(AppDynamicsIntegrationService appDynamicsService,
                                  AppDynamicsAuthService authService,
                                  MonitoringEventService monitoringEventService,
                                  ErrorPatternAnalysisService errorPatternService) {
        this.appDynamicsService = appDynamicsService;
        this.authService = authService;
        this.monitoringEventService = monitoringEventService;
        this.errorPatternService = errorPatternService;
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Collect business transactions every minute
     */
    @Scheduled(fixedRateString = "${appdynamics.api.business-transactions-interval:60000}")
    public void collectBusinessTransactions() {
        if (!isDataCollectionEnabled()) {
            return;
        }
        
        logger.debug("Starting business transactions collection from AppDynamics");
        
        try {
            if (parallelExecution) {
                CompletableFuture.runAsync(this::fetchBusinessTransactionsAsync, executorService);
            } else {
                fetchBusinessTransactionsSync();
            }
        } catch (Exception e) {
            logger.error("Error in business transactions collection", e);
        }
    }
    
    /**
     * Collect error snapshots every 30 seconds
     */
    @Scheduled(fixedRateString = "${appdynamics.api.error-snapshots-interval:30000}")
    public void collectErrorSnapshots() {
        if (!isDataCollectionEnabled()) {
            return;
        }
        
        logger.debug("Starting error snapshots collection from AppDynamics");
        
        try {
            if (parallelExecution) {
                CompletableFuture.runAsync(this::fetchErrorSnapshotsAsync, executorService);
            } else {
                fetchErrorSnapshotsSync();
            }
        } catch (Exception e) {
            logger.error("Error in error snapshots collection", e);
        }
    }
    
    /**
     * Collect performance metrics every minute
     */
    @Scheduled(fixedRateString = "${appdynamics.api.performance-metrics-interval:60000}")
    public void collectPerformanceMetrics() {
        if (!isDataCollectionEnabled()) {
            return;
        }
        
        logger.debug("Starting performance metrics collection from AppDynamics");
        
        try {
            if (parallelExecution) {
                CompletableFuture.runAsync(this::fetchPerformanceMetricsAsync, executorService);
            } else {
                fetchPerformanceMetricsSync();
            }
        } catch (Exception e) {
            logger.error("Error in performance metrics collection", e);
        }
    }
    
    /**
     * Collect health rule violations every 30 seconds
     */
    @Scheduled(fixedRateString = "${appdynamics.api.health-violations-interval:30000}")
    public void collectHealthViolations() {
        if (!isDataCollectionEnabled()) {
            return;
        }
        
        logger.debug("Starting health violations collection from AppDynamics");
        
        try {
            if (parallelExecution) {
                CompletableFuture.runAsync(this::fetchHealthViolationsAsync, executorService);
            } else {
                fetchHealthViolationsSync();
            }
        } catch (Exception e) {
            logger.error("Error in health violations collection", e);
        }
    }
    
    /**
     * Token maintenance - check and refresh if needed every 5 minutes
     */
    @Scheduled(fixedRateString = "${appdynamics.oauth2.token-check-interval:300000}")
    public void maintainToken() {
        if (!isDataCollectionEnabled()) {
            return;
        }
        
        try {
            AppDynamicsAuthService.TokenInfo tokenInfo = authService.getTokenInfo();
            
            logger.debug("Token maintenance check - Has token: {}, Expires in: {} seconds", 
                        tokenInfo.hasToken(), tokenInfo.getSecondsUntilExpiry());
            
            // Log token status for monitoring
            if (tokenInfo.hasToken()) {
                long secondsUntilExpiry = tokenInfo.getSecondsUntilExpiry();
                if (secondsUntilExpiry < 300) { // Less than 5 minutes
                    logger.info("AppDynamics token expires in {} seconds, will be refreshed automatically", 
                               secondsUntilExpiry);
                }
            } else {
                logger.warn("No AppDynamics token available");
            }
            
            // The auth service handles automatic refresh, but we can force validation
            if (!authService.validateToken()) {
                logger.warn("Token validation failed during maintenance check");
            }
            
        } catch (Exception e) {
            logger.error("Error during token maintenance", e);
        }
    }
    
    /**
     * Comprehensive data collection - runs every 5 minutes
     * Collects all types of data in sequence for correlation
     */
    @Scheduled(fixedRateString = "${appdynamics.data-collection.comprehensive-interval:300000}")
    public void comprehensiveDataCollection() {
        if (!isDataCollectionEnabled()) {
            return;
        }
        
        logger.info("Starting comprehensive AppDynamics data collection");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Collect all data types
            List<MonitoringEvent> businessTransactions = appDynamicsService.fetchBusinessTransactions();
            List<MonitoringEvent> errors = appDynamicsService.fetchErrors(LocalDateTime.now().minusHours(1));
            List<MonitoringEvent> metrics = appDynamicsService.fetchPerformanceMetrics();
            
            // Process and correlate data
            int totalEvents = businessTransactions.size() + errors.size() + metrics.size();
            
            // Store events
            businessTransactions.forEach(monitoringEventService::saveEvent);
            errors.forEach(monitoringEventService::saveEvent);
            metrics.forEach(monitoringEventService::saveEvent);
            
            // Analyze error patterns for auto-fixing
            if (!errors.isEmpty()) {
                errors.forEach(errorPatternService::analyzeErrorPattern);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Comprehensive data collection completed: {} events in {}ms", 
                       totalEvents, duration);
            
            // Create a monitoring event for the collection process itself
            MonitoringEvent collectionEvent = new MonitoringEvent(
                "appdynamics-collector", "data-collection", "info", "intelligent-monitoring-service");
            collectionEvent.setDescription("Comprehensive AppDynamics data collection");
            collectionEvent.getMetrics().put("totalEvents", totalEvents);
            collectionEvent.getMetrics().put("duration", duration);
            collectionEvent.getMetrics().put("businessTransactions", businessTransactions.size());
            collectionEvent.getMetrics().put("errors", errors.size());
            collectionEvent.getMetrics().put("metrics", metrics.size());
            
            monitoringEventService.saveEvent(collectionEvent);
            
        } catch (Exception e) {
            logger.error("Error during comprehensive data collection", e);
            
            // Create error event for monitoring
            MonitoringEvent errorEvent = new MonitoringEvent(
                "appdynamics-collector", "error", "high", "intelligent-monitoring-service");
            errorEvent.setDescription("AppDynamics data collection failed");
            errorEvent.setErrorSignature(e.getClass().getSimpleName());
            errorEvent.setStackTrace(getStackTrace(e));
            
            monitoringEventService.saveEvent(errorEvent);
        }
    }
    
    // Private helper methods for async execution
    
    private void fetchBusinessTransactionsAsync() {
        try {
            List<MonitoringEvent> events = appDynamicsService.fetchBusinessTransactions();
            events.forEach(monitoringEventService::saveEvent);
            logger.debug("Collected {} business transactions", events.size());
        } catch (Exception e) {
            logger.error("Error fetching business transactions asynchronously", e);
        }
    }
    
    private void fetchBusinessTransactionsSync() {
        List<MonitoringEvent> events = appDynamicsService.fetchBusinessTransactions();
        events.forEach(monitoringEventService::saveEvent);
        logger.debug("Collected {} business transactions", events.size());
    }
    
    private void fetchErrorSnapshotsAsync() {
        try {
            // Get recent business transactions first, then fetch their error snapshots
            List<MonitoringEvent> transactions = appDynamicsService.fetchBusinessTransactions();
            
            for (MonitoringEvent transaction : transactions) {
                String btId = transaction.getMetrics().get("businessTransactionId").toString();
                if (btId != null) {
                    List<MonitoringEvent> snapshots = appDynamicsService.fetchErrorSnapshots(btId);
                    snapshots.forEach(monitoringEventService::saveEvent);
                }
            }
            
            logger.debug("Collected error snapshots for {} business transactions", transactions.size());
        } catch (Exception e) {
            logger.error("Error fetching error snapshots asynchronously", e);
        }
    }
    
    private void fetchErrorSnapshotsSync() {
        // Similar implementation but synchronous
        List<MonitoringEvent> transactions = appDynamicsService.fetchBusinessTransactions();
        
        for (MonitoringEvent transaction : transactions) {
            String btId = transaction.getMetrics().get("businessTransactionId").toString();
            if (btId != null) {
                List<MonitoringEvent> snapshots = appDynamicsService.fetchErrorSnapshots(btId);
                snapshots.forEach(monitoringEventService::saveEvent);
            }
        }
        
        logger.debug("Collected error snapshots for {} business transactions", transactions.size());
    }
    
    private void fetchPerformanceMetricsAsync() {
        try {
            List<MonitoringEvent> events = appDynamicsService.fetchPerformanceMetrics();
            events.forEach(monitoringEventService::saveEvent);
            logger.debug("Collected {} performance metrics", events.size());
        } catch (Exception e) {
            logger.error("Error fetching performance metrics asynchronously", e);
        }
    }
    
    private void fetchPerformanceMetricsSync() {
        List<MonitoringEvent> events = appDynamicsService.fetchPerformanceMetrics();
        events.forEach(monitoringEventService::saveEvent);
        logger.debug("Collected {} performance metrics", events.size());
    }
    
    private void fetchHealthViolationsAsync() {
        try {
            List<MonitoringEvent> events = appDynamicsService.fetchErrors(LocalDateTime.now().minusHours(1));
            events.forEach(monitoringEventService::saveEvent);
            
            // Analyze for error patterns
            if (!events.isEmpty()) {
                events.forEach(errorPatternService::analyzeErrorPattern);
            }
            
            logger.debug("Collected {} health violations", events.size());
        } catch (Exception e) {
            logger.error("Error fetching health violations asynchronously", e);
        }
    }
    
    private void fetchHealthViolationsSync() {
        List<MonitoringEvent> events = appDynamicsService.fetchErrors(LocalDateTime.now().minusHours(1));
        events.forEach(monitoringEventService::saveEvent);
        
        // Analyze for error patterns
        if (!events.isEmpty()) {
            events.forEach(errorPatternService::analyzeErrorPattern);
        }
        
        logger.debug("Collected {} health violations", events.size());
    }
    
    private boolean isDataCollectionEnabled() {
        if (!dataCollectionEnabled) {
            return false;
        }
        
        if (!authService.isConfigured()) {
            logger.debug("AppDynamics not configured, skipping data collection");
            return false;
        }
        
        return true;
    }
    
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}