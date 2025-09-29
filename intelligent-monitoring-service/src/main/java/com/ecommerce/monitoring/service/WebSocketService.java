package com.ecommerce.monitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Service for real-time WebSocket communication
 */
@Service
public class WebSocketService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    
    private final ObjectMapper objectMapper;
    
    // Store active WebSocket sessions by channel
    private final Map<String, Set<WebSocketSession>> channelSessions = new ConcurrentHashMap<>();
    
    public WebSocketService() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Register a WebSocket session for a specific channel
     */
    public void registerSession(String channel, WebSocketSession session) {
        channelSessions.computeIfAbsent(channel, k -> new CopyOnWriteArraySet<>()).add(session);
        logger.info("WebSocket session registered for channel: {} (Session ID: {})", 
                   channel, session.getId());
    }
    
    /**
     * Unregister a WebSocket session
     */
    public void unregisterSession(String channel, WebSocketSession session) {
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                channelSessions.remove(channel);
            }
        }
        logger.info("WebSocket session unregistered from channel: {} (Session ID: {})", 
                   channel, session.getId());
    }
    
    /**
     * Broadcast message to all sessions in a channel
     */
    public void broadcast(String channel, Object message) {
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions == null || sessions.isEmpty()) {
            logger.debug("No active sessions for channel: {}", channel);
            return;
        }
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);
            
            int successCount = 0;
            int failCount = 0;
            
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                        successCount++;
                    } else {
                        // Remove closed sessions
                        sessions.remove(session);
                        failCount++;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to send WebSocket message to session: {}", session.getId(), e);
                    sessions.remove(session);
                    failCount++;
                }
            }
            
            logger.debug("Broadcast to channel {}: {} successful, {} failed", 
                        channel, successCount, failCount);
            
        } catch (Exception e) {
            logger.error("Failed to serialize message for broadcast to channel: {}", channel, e);
        }
    }
    
    /**
     * Send message to specific session
     */
    public void sendToSession(WebSocketSession session, Object message) {
        try {
            if (session.isOpen()) {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                logger.debug("Message sent to session: {}", session.getId());
            } else {
                logger.warn("Attempted to send message to closed session: {}", session.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to send message to session: {}", session.getId(), e);
        }
    }
    
    /**
     * Get active session count for a channel
     */
    public int getActiveSessionCount(String channel) {
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        return sessions != null ? sessions.size() : 0;
    }
    
    /**
     * Get all active channels
     */
    public Set<String> getActiveChannels() {
        return channelSessions.keySet();
    }
    
    /**
     * Send real-time monitoring alert
     */
    public void sendMonitoringAlert(String severity, String message, Map<String, Object> details) {
        Map<String, Object> alert = Map.of(
            "type", "monitoring_alert",
            "severity", severity,
            "message", message,
            "details", details,
            "timestamp", java.time.LocalDateTime.now()
        );
        
        broadcast("monitoring-alerts", alert);
        
        // Also send to admin notifications if it's critical
        if ("critical".equalsIgnoreCase(severity)) {
            broadcast("admin-notifications", alert);
        }
    }
    
    /**
     * Send real-time system status update
     */
    public void sendSystemStatus(String component, String status, Map<String, Object> metrics) {
        Map<String, Object> statusUpdate = Map.of(
            "type", "system_status",
            "component", component,
            "status", status,
            "metrics", metrics,
            "timestamp", java.time.LocalDateTime.now()
        );
        
        broadcast("system-status", statusUpdate);
    }
    
    /**
     * Send real-time code review notification
     */
    public void sendCodeReviewNotification(String reviewId, String action, Map<String, Object> details) {
        Map<String, Object> notification = Map.of(
            "type", "code_review_notification",
            "reviewId", reviewId,
            "action", action,
            "details", details,
            "timestamp", java.time.LocalDateTime.now()
        );
        
        broadcast("code-reviews", notification);
        broadcast("admin-notifications", notification);
    }
}