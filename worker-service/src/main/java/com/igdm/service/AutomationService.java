package com.igdm.service;

import com.igdm.entity.AutomationRule;
import com.igdm.entity.InstagramAccount;
import com.igdm.entity.InteractionLog;
import com.igdm.dto.CommentEventMessage;
import com.igdm.dto.CommentEventMessage.CommentPayload;
import com.igdm.repository.AutomationRuleRepository;
import com.igdm.repository.InstagramAccountRepository;
import com.igdm.repository.InteractionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core business logic for the comment-to-DM automation pipeline.
 *
 * Responsibilities:
 * 1. Deduplication — skip already-processed comments
 * 2. Account lookup — find the InstagramAccount by igUserId
 * 3. Rule matching — find active rules and check trigger conditions
 * 4. Template rendering — substitute placeholders in DM template
 * 5. Daily limit enforcement — prevent exceeding per-rule DM limits
 * 6. Interaction logging — record every processing outcome
 */
@Service
public class AutomationService {

    private static final Logger log = LoggerFactory.getLogger(AutomationService.class);

    private final InstagramAccountRepository igAccountRepo;
    private final AutomationRuleRepository ruleRepo;
    private final InteractionLogRepository logRepo;

    public AutomationService(
            InstagramAccountRepository igAccountRepo,
            AutomationRuleRepository ruleRepo,
            InteractionLogRepository logRepo) {
        this.igAccountRepo = igAccountRepo;
        this.ruleRepo = ruleRepo;
        this.logRepo = logRepo;
    }

    /**
     * Result of processing a comment event.
     */
    public record ProcessingResult(
            String status,
            String renderedDm,
            AutomationRule matchedRule,
            InteractionLog interactionLog
    ) {
        public static ProcessingResult skipped(String reason, InteractionLog interactionLog) {
            return new ProcessingResult("SKIPPED", reason, null, interactionLog);
        }

        public static ProcessingResult matched(String renderedDm, AutomationRule rule, InteractionLog interactionLog) {
            return new ProcessingResult("MATCHED", renderedDm, rule, interactionLog);
        }

        public static ProcessingResult rateLimited(AutomationRule rule, InteractionLog interactionLog) {
            return new ProcessingResult("RATE_LIMITED", null, rule, interactionLog);
        }

        public boolean shouldSendDm() {
            return "MATCHED".equals(status);
        }
    }

    /**
     * Process a comment event: dedup, match rules, render template.
     * Does NOT send the DM — that's handled by the consumer after this returns.
     *
     * @param message The deserialized comment event from the queue
     * @return ProcessingResult indicating what action to take
     */
    @Transactional
    public ProcessingResult processComment(CommentEventMessage message) {
        CommentPayload payload = message.getPayload();

        if (payload == null || payload.getCommentId() == null) {
            log.warn("Received message with null payload or commentId: {}", message.getMessageId());
            return ProcessingResult.skipped("Invalid payload", null);
        }

        // --- 1. Deduplication ---
        if (logRepo.existsByIgCommentId(payload.getCommentId())) {
            log.info("Duplicate comment detected, skipping: {}", payload.getCommentId());
            return ProcessingResult.skipped("Duplicate comment", null);
        }

        // --- 2. Create initial interaction log (QUEUED) ---
        InteractionLog interactionLog = new InteractionLog();
        interactionLog.setIgCommentId(payload.getCommentId());
        interactionLog.setIgCommenterId(payload.getCommenter() != null ? payload.getCommenter().getId() : "unknown");
        interactionLog.setIgCommenterUsername(payload.getCommenter() != null ? payload.getCommenter().getUsername() : null);
        interactionLog.setCommentText(payload.getCommentText());
        interactionLog.setMediaId(payload.getMediaId());
        interactionLog.setStatus("QUEUED");
        interactionLog = logRepo.save(interactionLog);

        // --- 3. Look up Instagram account ---
        Optional<InstagramAccount> accountOpt = igAccountRepo.findByIgUserId(payload.getIgAccountId());
        if (accountOpt.isEmpty()) {
            log.warn("No Instagram account found for igUserId: {}", payload.getIgAccountId());
            interactionLog.setStatus("SKIPPED");
            interactionLog.setErrorMessage("Instagram account not found");
            logRepo.save(interactionLog);
            return ProcessingResult.skipped("Account not found", interactionLog);
        }

        InstagramAccount account = accountOpt.get();
        if (!account.isConnected()) {
            log.warn("Instagram account {} is disconnected", account.getIgUserId());
            interactionLog.setStatus("SKIPPED");
            interactionLog.setErrorMessage("Account disconnected");
            logRepo.save(interactionLog);
            return ProcessingResult.skipped("Account disconnected", interactionLog);
        }

        // --- 4. Find matching active rules ---
        List<AutomationRule> activeRules = ruleRepo.findByIgAccountIdAndIsActiveTrue(account.getId());
        if (activeRules.isEmpty()) {
            log.debug("No active rules for account: {}", account.getIgUserId());
            interactionLog.setStatus("SKIPPED");
            interactionLog.setErrorMessage("No active rules");
            logRepo.save(interactionLog);
            return ProcessingResult.skipped("No active rules", interactionLog);
        }

        // --- 5. Check each rule for a trigger match ---
        for (AutomationRule rule : activeRules) {
            if (matchesTrigger(rule, payload)) {
                // --- 6. Check daily limit ---
                if (rule.getDmSentToday() >= rule.getDailyDmLimit()) {
                    log.warn("Daily DM limit reached for rule '{}' ({}/{})",
                            rule.getName(), rule.getDmSentToday(), rule.getDailyDmLimit());
                    interactionLog.setRule(rule);
                    interactionLog.setStatus("RATE_LIMITED");
                    interactionLog.setErrorMessage("Daily DM limit reached");
                    logRepo.save(interactionLog);
                    return ProcessingResult.rateLimited(rule, interactionLog);
                }

                // --- 7. Check first_comment dedup ---
                if ("first_comment".equals(rule.getTriggerType())) {
                    String commenterId = payload.getCommenter() != null ? payload.getCommenter().getId() : null;
                    if (commenterId != null &&
                            logRepo.existsByIgCommenterIdAndRuleIdAndStatus(commenterId, rule.getId(), "SENT")) {
                        log.debug("Commenter {} already received DM from rule {}", commenterId, rule.getName());
                        continue; // Try next rule
                    }
                }

                // --- 8. Render DM template ---
                String renderedDm = renderTemplate(rule.getDmTemplate(), payload);

                interactionLog.setRule(rule);
                interactionLog.setDmContentSent(renderedDm);
                logRepo.save(interactionLog);

                log.info("Comment {} matched rule '{}' — DM ready to send",
                        payload.getCommentId(), rule.getName());

                return ProcessingResult.matched(renderedDm, rule, interactionLog);
            }
        }

        // No rule matched
        log.debug("No rules matched comment {}", payload.getCommentId());
        interactionLog.setStatus("SKIPPED");
        interactionLog.setErrorMessage("No matching trigger");
        logRepo.save(interactionLog);
        return ProcessingResult.skipped("No matching trigger", interactionLog);
    }

    /**
     * Check if a comment matches a rule's trigger condition.
     */
    boolean matchesTrigger(AutomationRule rule, CommentPayload payload) {
        return switch (rule.getTriggerType()) {
            case "any_comment" -> true;
            case "first_comment" -> true; // Dedup checked separately above
            case "keyword" -> matchesKeywords(rule.getTriggerKeywords(), payload.getCommentText());
            default -> {
                log.warn("Unknown trigger type: {}", rule.getTriggerType());
                yield false;
            }
        };
    }

    /**
     * Case-insensitive keyword matching.
     * Returns true if the comment text contains any of the trigger keywords.
     */
    boolean matchesKeywords(List<String> keywords, String commentText) {
        if (keywords == null || keywords.isEmpty() || commentText == null) {
            return false;
        }
        String lowerComment = commentText.toLowerCase();
        return keywords.stream()
                .anyMatch(keyword -> lowerComment.contains(keyword.toLowerCase()));
    }

    /**
     * Render a DM template by replacing placeholders with actual values.
     *
     * Supported placeholders:
     *   {{username}}     → commenter's Instagram username
     *   {{comment_text}} → the comment text
     *   {{media_id}}     → the IG media (post/reel) ID
     */
    String renderTemplate(String template, CommentPayload payload) {
        String rendered = template;

        String username = (payload.getCommenter() != null && payload.getCommenter().getUsername() != null)
                ? payload.getCommenter().getUsername()
                : "there";
        rendered = rendered.replace("{{username}}", username);
        rendered = rendered.replace("{{comment_text}}", payload.getCommentText() != null ? payload.getCommentText() : "");
        rendered = rendered.replace("{{media_id}}", payload.getMediaId() != null ? payload.getMediaId() : "");

        return rendered;
    }

    /**
     * Mark an interaction as successfully sent.
     */
    @Transactional
    public void markAsSent(InteractionLog interactionLog, AutomationRule rule) {
        interactionLog.setStatus("SENT");
        interactionLog.setDmSentAt(OffsetDateTime.now());
        logRepo.save(interactionLog);

        // Atomically increment the daily counter
        ruleRepo.incrementDmSentToday(rule.getId());

        log.info("DM sent successfully for comment {}", interactionLog.getIgCommentId());
    }

    /**
     * Mark an interaction as failed.
     */
    @Transactional
    public void markAsFailed(InteractionLog interactionLog, String errorMessage) {
        interactionLog.setStatus("FAILED");
        interactionLog.setErrorMessage(errorMessage);
        interactionLog.setRetryCount(interactionLog.getRetryCount() + 1);
        logRepo.save(interactionLog);

        log.error("DM failed for comment {}: {}", interactionLog.getIgCommentId(), errorMessage);
    }
}
