package com.igdm.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igdm.config.RabbitConfig;
import com.igdm.dto.CommentEventMessage;
import com.igdm.entity.InstagramAccount;
import com.igdm.repository.InstagramAccountRepository;
import com.igdm.service.AutomationService;
import com.igdm.service.AutomationService.ProcessingResult;
import com.igdm.service.RetryService;
import com.igdm.service.MetaGraphApiService;
import com.igdm.service.MetaGraphApiService.SendDmResult;
import com.igdm.service.TokenEncryptionService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * RabbitMQ consumer for Instagram comment events.
 *
 * Lifecycle per message:
 * 1. Deserialize JSON → CommentEventMessage
 * 2. Delegate to AutomationService for rule matching
 * 3. If matched → send DM via MetaGraphApiService
 * 4. ACK on success, retry with backoff on transient failure, DLQ on permanent failure
 *
 * Uses manual acknowledgement mode for precise retry control:
 * - basicAck:    message processed successfully → remove from queue
 * - RetryService: transient error → exponential backoff via per-message TTL
 * - RetryService: max retries → dead-letter queue
 */
@Component
public class CommentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CommentEventConsumer.class);

    private final AutomationService automationService;
    private final MetaGraphApiService metaApiService;
    private final TokenEncryptionService encryptionService;
    private final RetryService retryService;
    private final InstagramAccountRepository igAccountRepo;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public CommentEventConsumer(
            AutomationService automationService,
            MetaGraphApiService metaApiService,
            TokenEncryptionService encryptionService,
            RetryService retryService,
            InstagramAccountRepository igAccountRepo,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper) {
        this.automationService = automationService;
        this.metaApiService = metaApiService;
        this.encryptionService = encryptionService;
        this.retryService = retryService;
        this.igAccountRepo = igAccountRepo;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.PROCESS_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleCommentEvent(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        CommentEventMessage event = null;

        try {
            // --- 1. Deserialize ---
            event = objectMapper.readValue(message.getBody(), CommentEventMessage.class);
            int retryCount = retryService.getRetryCount(message);

            log.info("Received comment event: messageId={}, commentId={}, retry={}/{}",
                    event.getMessageId(),
                    event.getPayload() != null ? event.getPayload().getCommentId() : "null",
                    retryCount, retryService.getMaxRetries());

            // --- 2. Process (dedup, match, render) ---
            ProcessingResult result = automationService.processComment(event);

            if (!result.shouldSendDm()) {
                log.info("Comment {} — result: {} ({})",
                        event.getPayload().getCommentId(), result.status(), result.renderedDm());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // --- 3. Send DM via Meta Graph API ---
            String igAccountId = event.getPayload().getIgAccountId();
            String recipientId = event.getPayload().getCommenter().getId();
            String dmText = result.renderedDm();
            Instant commentTimestamp = event.getPayload().getCommentTimestamp();

            // Look up the IG account to get the decrypted access token
            Optional<InstagramAccount> accountOpt = igAccountRepo.findByIgUserId(igAccountId);
            if (accountOpt.isEmpty()) {
                log.error("IG account not found for igUserId={} — cannot send DM", igAccountId);
                automationService.markAsFailed(result.interactionLog(), "IG account not found");
                channel.basicAck(deliveryTag, false);
                return;
            }

            InstagramAccount account = accountOpt.get();
            String accessToken = encryptionService.decrypt(account.getAccessTokenEncrypted());

            // Send the DM
            SendDmResult dmResult = metaApiService.sendDirectMessage(
                    igAccountId, recipientId, dmText, accessToken, commentTimestamp);

            if (dmResult.success()) {
                automationService.markAsSent(result.interactionLog(), result.matchedRule());
                channel.basicAck(deliveryTag, false);

            } else if (dmResult.isRateLimited() || dmResult.shouldRetry()) {
                // Transient/rate-limit error — retry with exponential backoff
                String reason = dmResult.isRateLimited() ? "Rate limited" : dmResult.errorMessage();
                log.warn("DM failed for comment {} ({}), scheduling retry",
                        event.getPayload().getCommentId(), reason);
                automationService.markAsFailed(result.interactionLog(), reason);

                // ACK the original, then publish to retry queue with backoff TTL
                channel.basicAck(deliveryTag, false);
                retryService.retryWithBackoff(message);

            } else if (dmResult.isTokenError()) {
                // Token invalid — mark account as disconnected, don't retry
                log.error("Token error for IG account {} — marking disconnected", igAccountId);
                account.setConnected(false);
                igAccountRepo.save(account);
                automationService.markAsFailed(result.interactionLog(),
                        "Token expired: " + dmResult.errorMessage());
                channel.basicAck(deliveryTag, false);

            } else {
                // Permanent error — ACK and log failure
                automationService.markAsFailed(result.interactionLog(), dmResult.errorMessage());
                channel.basicAck(deliveryTag, false);
            }

        } catch (Exception e) {
            log.error("Error processing comment event: {}", e.getMessage(), e);
            handleFailure(message, channel, deliveryTag, e);
        }
    }

    /**
     * Handle unexpected processing failures with retry.
     * Uses RetryService for exponential backoff.
     */
    private void handleFailure(
            Message message,
            Channel channel,
            long deliveryTag,
            Exception error) throws IOException {

        // ACK the original message (we'll re-publish it ourselves)
        channel.basicAck(deliveryTag, false);

        if (retryService.canRetry(message)) {
            int retryCount = retryService.getRetryCount(message);
            long delay = retryService.getBackoffDelay(retryCount);
            log.warn("Transient failure (retry {}/{}), backoff={}ms: {}",
                    retryCount + 1, retryService.getMaxRetries(), delay, error.getMessage());
            retryService.retryWithBackoff(message);
        } else {
            log.error("Max retries exhausted — sending to dead-letter queue: {}", error.getMessage());
            retryService.sendToDeadLetter(message, retryService.getRetryCount(message));
        }
    }
}

