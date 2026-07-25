package com.igdm.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks every comment event processed by the system.
 * Serves as both an audit log and a deduplication table (ig_comment_id is unique).
 */
@Entity
@Table(name = "interaction_logs")
public class InteractionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private AutomationRule rule;

    /**
     * Instagram comment ID — unique constraint ensures we never process the same comment twice.
     */
    @Column(name = "ig_comment_id", nullable = false, unique = true)
    private String igCommentId;

    @Column(name = "ig_commenter_id", nullable = false)
    private String igCommenterId;

    @Column(name = "ig_commenter_username")
    private String igCommenterUsername;

    @Column(name = "comment_text")
    private String commentText;

    @Column(name = "media_id")
    private String mediaId;

    /**
     * Processing status:
     * QUEUED → SENT | FAILED | SKIPPED | RATE_LIMITED
     */
    @Column(nullable = false)
    private String status = "QUEUED";

    /**
     * The rendered DM text that was actually sent (after template substitution).
     */
    @Column(name = "dm_content_sent")
    private String dmContentSent;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "dm_sent_at")
    private OffsetDateTime dmSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AutomationRule getRule() { return rule; }
    public void setRule(AutomationRule rule) { this.rule = rule; }

    public String getIgCommentId() { return igCommentId; }
    public void setIgCommentId(String igCommentId) { this.igCommentId = igCommentId; }

    public String getIgCommenterId() { return igCommenterId; }
    public void setIgCommenterId(String igCommenterId) { this.igCommenterId = igCommenterId; }

    public String getIgCommenterUsername() { return igCommenterUsername; }
    public void setIgCommenterUsername(String igCommenterUsername) { this.igCommenterUsername = igCommenterUsername; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDmContentSent() { return dmContentSent; }
    public void setDmContentSent(String dmContentSent) { this.dmContentSent = dmContentSent; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public OffsetDateTime getDmSentAt() { return dmSentAt; }
    public void setDmSentAt(OffsetDateTime dmSentAt) { this.dmSentAt = dmSentAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
