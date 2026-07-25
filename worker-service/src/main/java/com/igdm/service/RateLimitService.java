package com.igdm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks Meta Graph API rate limit state from response headers.
 *
 * Meta uses two types of rate limiting:
 * 1. x-app-usage: App-level limits (call_count, total_cputime, total_time as %)
 * 2. x-business-use-case-usage: Per-business/object limits
 *
 * Strategy:
 * - Proactive throttling at 80% usage (configurable THROTTLE_THRESHOLD)
 * - When throttled, the consumer defers messages back to the retry queue
 * - Hard rate-limit (100%) is detected from HTTP 429 responses
 * - Throttle state auto-expires after THROTTLE_COOLDOWN_SECONDS
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    /**
     * Percentage threshold at which to start proactive throttling.
     * At 80%, we slow down to avoid hitting the hard 100% limit.
     */
    private static final int THROTTLE_THRESHOLD = 80;

    /**
     * Seconds to wait before retrying after hitting a rate limit.
     */
    private static final int THROTTLE_COOLDOWN_SECONDS = 300; // 5 minutes

    private final AtomicInteger currentCallCount = new AtomicInteger(0);
    private final AtomicInteger currentCpuTime = new AtomicInteger(0);
    private final AtomicInteger currentTotalTime = new AtomicInteger(0);
    private final AtomicReference<Instant> throttledUntil = new AtomicReference<>(null);

    private final ObjectMapper objectMapper;

    public RateLimitService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Check if the service is currently in a throttled state.
     * Returns true if any usage metric exceeds the threshold or if
     * we're in a cooldown period after a hard rate limit.
     */
    public boolean isThrottled() {
        // Check cooldown from hard rate limit
        Instant until = throttledUntil.get();
        if (until != null && Instant.now().isBefore(until)) {
            log.debug("Rate limited — cooldown active until {}", until);
            return true;
        }

        // Check proactive threshold
        boolean exceeded = currentCallCount.get() >= THROTTLE_THRESHOLD
                || currentCpuTime.get() >= THROTTLE_THRESHOLD
                || currentTotalTime.get() >= THROTTLE_THRESHOLD;

        if (exceeded) {
            log.debug("Proactive throttle — call_count={}%, cputime={}%, total_time={}%",
                    currentCallCount.get(), currentCpuTime.get(), currentTotalTime.get());
        }

        return exceeded;
    }

    /**
     * Update rate limit state from the x-app-usage response header.
     *
     * Header value format:
     * {"call_count":28,"total_cputime":5,"total_time":12}
     *
     * @param headerValue The raw x-app-usage header value
     */
    public void updateFromAppUsageHeader(String headerValue) {
        try {
            JsonNode node = objectMapper.readTree(headerValue);

            int callCount = node.has("call_count") ? node.get("call_count").asInt() : 0;
            int cpuTime = node.has("total_cputime") ? node.get("total_cputime").asInt() : 0;
            int totalTime = node.has("total_time") ? node.get("total_time").asInt() : 0;

            currentCallCount.set(callCount);
            currentCpuTime.set(cpuTime);
            currentTotalTime.set(totalTime);

            log.debug("x-app-usage updated: call_count={}%, total_cputime={}%, total_time={}%",
                    callCount, cpuTime, totalTime);

            // If any metric exceeds 80%, log a warning
            if (callCount >= THROTTLE_THRESHOLD || cpuTime >= THROTTLE_THRESHOLD || totalTime >= THROTTLE_THRESHOLD) {
                log.warn("⚠ Rate limit approaching: call_count={}%, total_cputime={}%, total_time={}%",
                        callCount, cpuTime, totalTime);
            }

        } catch (Exception e) {
            log.warn("Failed to parse x-app-usage header: {}", headerValue, e);
        }
    }

    /**
     * Update rate limit state from the x-business-use-case-usage response header.
     * This header has a more complex structure per business/object ID.
     *
     * @param headerValue The raw x-business-use-case-usage header value
     */
    public void updateFromBusinessUsageHeader(String headerValue) {
        try {
            JsonNode root = objectMapper.readTree(headerValue);

            // Iterate through business IDs
            root.fields().forEachRemaining(entry -> {
                JsonNode usageArray = entry.getValue();
                if (usageArray.isArray()) {
                    for (JsonNode usage : usageArray) {
                        int callCount = usage.has("call_count") ? usage.get("call_count").asInt() : 0;
                        int cpuTime = usage.has("total_cputime") ? usage.get("total_cputime").asInt() : 0;
                        int totalTime = usage.has("total_time") ? usage.get("total_time").asInt() : 0;

                        // Use the highest values across all business objects
                        currentCallCount.updateAndGet(current -> Math.max(current, callCount));
                        currentCpuTime.updateAndGet(current -> Math.max(current, cpuTime));
                        currentTotalTime.updateAndGet(current -> Math.max(current, totalTime));
                    }
                }
            });

        } catch (Exception e) {
            log.warn("Failed to parse x-business-use-case-usage header: {}", headerValue, e);
        }
    }

    /**
     * Mark the service as hard rate-limited.
     * Called when we receive an HTTP 429 or error code 4/32 from Meta.
     * Activates a cooldown period during which all API calls are deferred.
     */
    public void markRateLimited() {
        Instant until = Instant.now().plusSeconds(THROTTLE_COOLDOWN_SECONDS);
        throttledUntil.set(until);
        log.error("🚨 Hard rate limit activated — all API calls deferred until {}", until);
    }

    /**
     * Get the current rate limit state for monitoring/logging.
     */
    public RateLimitState getCurrentState() {
        return new RateLimitState(
                currentCallCount.get(),
                currentCpuTime.get(),
                currentTotalTime.get(),
                isThrottled(),
                throttledUntil.get()
        );
    }

    /**
     * Immutable snapshot of rate limit state for monitoring.
     */
    public record RateLimitState(
            int callCountPercent,
            int cpuTimePercent,
            int totalTimePercent,
            boolean isThrottled,
            Instant throttledUntil
    ) {}
}
