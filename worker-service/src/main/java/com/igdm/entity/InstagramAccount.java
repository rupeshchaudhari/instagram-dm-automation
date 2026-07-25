package com.igdm.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a connected Instagram Business/Creator account.
 * Stores the Meta Graph API access token (encrypted) and account metadata.
 */
@Entity
@Table(name = "instagram_accounts")
public class InstagramAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Instagram-scoped user ID (IGSID).
     * This is the primary identifier used in webhook payloads (entry.id).
     */
    @Column(name = "ig_user_id", nullable = false, unique = true)
    private String igUserId;

    @Column(name = "ig_username")
    private String igUsername;

    /**
     * Facebook Page ID linked to this Instagram account.
     * Required for sending DMs via the Pages API.
     */
    @Column(name = "page_id")
    private String pageId;

    /**
     * Long-lived access token, encrypted with AES-256 at rest.
     * Decrypted only when making Graph API calls.
     */
    @Column(name = "access_token_encrypted", nullable = false)
    private String accessTokenEncrypted;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    @Column(name = "is_connected", nullable = false)
    private boolean isConnected = true;

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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getIgUserId() { return igUserId; }
    public void setIgUserId(String igUserId) { this.igUserId = igUserId; }

    public String getIgUsername() { return igUsername; }
    public void setIgUsername(String igUsername) { this.igUsername = igUsername; }

    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }

    public String getAccessTokenEncrypted() { return accessTokenEncrypted; }
    public void setAccessTokenEncrypted(String accessTokenEncrypted) { this.accessTokenEncrypted = accessTokenEncrypted; }

    public OffsetDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(OffsetDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public boolean isConnected() { return isConnected; }
    public void setConnected(boolean connected) { isConnected = connected; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
