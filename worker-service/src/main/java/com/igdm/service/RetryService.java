package com.igdm.service;

import com.igdm.config.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igdm.dto.CommentEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Manages message retry with exponential backoff using RabbitMQ per-message TTL.
 *
 * Strategy:
 * Instead of using a single retry queue with a fixed TTL, we publish to the
 * retry exchange with a per-message TTL header. This gives us true exponential
 * backoff without needing multiple queues.
 *
 * Backoff schedule:
 *   Retry 1: 5 seconds
 *   Retry 2: 30 seconds
 *   Retry 3: 120 seconds (2 minutes)
 *   After 3:  → dead-letter queue
 *
 * Note: RabbitMQ per-message TTL only works when the message is at the head of
 * the queue. Since our retry queue typically has low throughput, this is acceptable.
 * For high-throughput scenarios, consider the delayed-message-exchange plugin.
 */
@Service
public class RetryService {

    private static final Logger log = LoggerFactory.getLogger(RetryService.class);

    /**
     * Backoff delays in milliseconds, indexed by retry attempt (0-based).
     */
    private static final long[] BACKOFF_DELAYS_MS = {
            5_000,    // Retry 1: 5 seconds
            30_000,   // Retry 2: 30 seconds
            120_000,  // Retry 3: 2 minutes
    };

    private static final int MAX_RETRIES = BACKOFF_DELAYS_MS.length;

    /**
     * Custom header to track retry count independently of x-death.
     * This is more reliable than parsing x-death when using per-message TTL.
     */
    private static final String RETRY_COUNT_HEADER = "x-igdm-retry-count";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RetryService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Check if the message can be retried.
     *
     * @param message The original AMQP message
     * @return true if retry count < max retries
     */
    public boolean canRetry(Message message) {
        int retryCount = getRetryCount(message);
        return retryCount < MAX_RETRIES;
    }

    /**
     * Send a message to the retry queue with exponential backoff TTL.
     *
     * @param originalMessage The original AMQP message that failed
     * @return true if sent to retry, false if max retries exceeded (sent to DLQ)
     */
    public boolean retryWithBackoff(Message originalMessage) {
        int retryCount = getRetryCount(originalMessage);

        if (retryCount >= MAX_RETRIES) {
            sendToDeadLetter(originalMessage, retryCount);
            return false;
        }

        long delayMs = BACKOFF_DELAYS_MS[retryCount];
        int nextRetryCount = retryCount + 1;

        log.info("Scheduling retry {}/{} with {}ms delay",
                nextRetryCount, MAX_RETRIES, delayMs);

        // Build a new message with updated retry count and per-message TTL
        Message retryMessage = MessageBuilder
                .withBody(originalMessage.getBody())
                .copyProperties(originalMessage.getMessageProperties())
                .setHeader(RETRY_COUNT_HEADER, nextRetryCount)
                .setExpiration(String.valueOf(delayMs))  // Per-message TTL
                .build();

        // Publish to the retry exchange → retry queue (with TTL) → DLX back to main
        rabbitTemplate.send(
                RabbitConfig.RETRY_EXCHANGE,
                RabbitConfig.COMMENT_RETRY_KEY,
                retryMessage
        );

        log.info("Message sent to retry queue: retry={}/{}, delay={}ms",
                nextRetryCount, MAX_RETRIES, delayMs);

        return true;
    }

    /**
     * Send a message to the dead-letter queue after max retries.
     *
     * @param message     The original message
     * @param retryCount  The final retry count
     */
    public void sendToDeadLetter(Message message, int retryCount) {
        log.error("Max retries ({}) exhausted — sending to dead-letter queue", retryCount);

        Message dlqMessage = MessageBuilder
                .withBody(message.getBody())
                .copyProperties(message.getMessageProperties())
                .setHeader(RETRY_COUNT_HEADER, retryCount)
                .setHeader("x-igdm-failure-reason", "Max retries exhausted")
                .setHeader("x-igdm-dead-lettered-at", System.currentTimeMillis())
                .build();

        rabbitTemplate.send(
                RabbitConfig.DLX_EXCHANGE,
                RabbitConfig.COMMENT_DEAD_KEY,
                dlqMessage
        );
    }

    /**
     * Extract retry count from our custom header.
     */
    public int getRetryCount(Message message) {
        Object count = message.getMessageProperties().getHeader(RETRY_COUNT_HEADER);
        if (count instanceof Integer) {
            return (Integer) count;
        }
        if (count instanceof Long) {
            return ((Long) count).intValue();
        }
        return 0;
    }

    /**
     * Get the maximum number of retries.
     */
    public int getMaxRetries() {
        return MAX_RETRIES;
    }

    /**
     * Get the backoff delay for a given retry attempt.
     */
    public long getBackoffDelay(int retryAttempt) {
        if (retryAttempt < 0 || retryAttempt >= BACKOFF_DELAYS_MS.length) {
            return BACKOFF_DELAYS_MS[BACKOFF_DELAYS_MS.length - 1];
        }
        return BACKOFF_DELAYS_MS[retryAttempt];
    }
}
