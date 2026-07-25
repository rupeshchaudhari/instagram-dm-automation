package com.igdm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System notification and alerting service.
 * Dispatches rate-limit threshold alerts and DLQ error notifications.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final Map<String, OffsetDateTime> lastAlertSent = new ConcurrentHashMap<>();

    /**
     * Trigger alert when Meta API usage exceeds safety threshold (80%).
     */
    public void sendRateLimitAlert(int callCountPercent, int cpuTimePercent, int totalTimePercent) {
        String key = "RATE_LIMIT";
        OffsetDateTime lastSent = lastAlertSent.get(key);

        // Throttle alert emails to once per 15 minutes
        if (lastSent != null && lastSent.isAfter(OffsetDateTime.now().minusMinutes(15))) {
            return;
        }

        log.warn("🚨 [ALERT] Meta Graph API Rate Limit threshold reached! CallCount: {}%, CPU: {}%, TotalTime: {}%. Throttling enabled.",
                callCountPercent, cpuTimePercent, totalTimePercent);

        lastAlertSent.put(key, OffsetDateTime.now());
    }

    /**
     * Trigger alert when a comment processing event fails and is routed to Dead-Letter Queue (DLQ).
     */
    public void sendDlqAlert(String commentId, String commenterUsername, String errorMessage, int retryCount) {
        log.error("🚨 [DLQ ALERT] Comment event {} (user: {}) failed after {} retries! Reason: {}. Moved to Dead-Letter Queue.",
                commentId, commenterUsername, retryCount, errorMessage);
    }
}
