package com.igdm.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Defines a comment-to-DM automation rule.
 *
 * Trigger types:
 * - "keyword": DM sent only if comment contains any of the trigger_keywords
 * - "any_comment": DM sent for every comment on the account's posts
 * - "first_comment": DM sent only for the first comment by a user (dedup via InteractionLog)
 */
@Entity
@Table(name = "automation_rules")
public class AutomationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ig_account_id", nullable = false)
    private InstagramAccount igAccount;

    @Column(nullable = false)
    private String name;

    @Column(name = "trigger_type", nullable = false)
    private String triggerType = "keyword";

    /**
     * List of trigger keywords stored as JSONB in PostgreSQL.
     * Hibernate 6 maps List<String> to JSONB via @JdbcTypeCode.
     */
    @Column(name = "trigger_keywords", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> triggerKeywords = new ArrayList<>();

    /**
     * DM message template with placeholder support.
     * Supported placeholders: {{username}}, {{comment_text}}, {{media_id}}
     */
    @Column(name = "dm_template", nullable = false)
    private String dmTemplate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "daily_dm_limit", nullable = false)
    private int dailyDmLimit = 100;

    /**
     * Counter tracking DMs sent today for this rule.
     * Reset to 0 daily by a scheduled cron job.
     */
    @Column(name = "dm_sent_today", nullable = false)
    private int dmSentToday = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public InstagramAccount getIgAccount() { return igAccount; }
    public void setIgAccount(InstagramAccount igAccount) { this.igAccount = igAccount; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public List<String> getTriggerKeywords() { return triggerKeywords; }
    public void setTriggerKeywords(List<String> triggerKeywords) { this.triggerKeywords = triggerKeywords; }

    public String getDmTemplate() { return dmTemplate; }
    public void setDmTemplate(String dmTemplate) { this.dmTemplate = dmTemplate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getDailyDmLimit() { return dailyDmLimit; }
    public void setDailyDmLimit(int dailyDmLimit) { this.dailyDmLimit = dailyDmLimit; }

    public int getDmSentToday() { return dmSentToday; }
    public void setDmSentToday(int dmSentToday) { this.dmSentToday = dmSentToday; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
