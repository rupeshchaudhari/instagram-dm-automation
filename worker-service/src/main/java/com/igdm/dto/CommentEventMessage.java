package com.igdm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * DTO matching the message envelope published by the Node.js webhook receiver.
 *
 * Structure:
 * {
 *   "messageId": "uuid",
 *   "timestamp": "ISO-8601",
 *   "eventType": "comment.created",
 *   "retryCount": 0,
 *   "maxRetries": 3,
 *   "payload": { ... },
 *   "rawWebhookEntry": { ... }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentEventMessage {

    private String messageId;
    private Instant timestamp;
    private String eventType;
    private int retryCount;
    private int maxRetries;
    private CommentPayload payload;

    // --- Nested Payload ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommentPayload {
        private String igAccountId;
        private String mediaId;
        private String commentId;
        private String commentText;
        private Instant commentTimestamp;
        private Commenter commenter;

        // Getters and Setters
        public String getIgAccountId() { return igAccountId; }
        public void setIgAccountId(String igAccountId) { this.igAccountId = igAccountId; }

        public String getMediaId() { return mediaId; }
        public void setMediaId(String mediaId) { this.mediaId = mediaId; }

        public String getCommentId() { return commentId; }
        public void setCommentId(String commentId) { this.commentId = commentId; }

        public String getCommentText() { return commentText; }
        public void setCommentText(String commentText) { this.commentText = commentText; }

        public Instant getCommentTimestamp() { return commentTimestamp; }
        public void setCommentTimestamp(Instant commentTimestamp) { this.commentTimestamp = commentTimestamp; }

        public Commenter getCommenter() { return commenter; }
        public void setCommenter(Commenter commenter) { this.commenter = commenter; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Commenter {
        private String id;
        private String username;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    // --- Envelope Getters and Setters ---

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public CommentPayload getPayload() { return payload; }
    public void setPayload(CommentPayload payload) { this.payload = payload; }

    @Override
    public String toString() {
        return "CommentEventMessage{" +
                "messageId='" + messageId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", retryCount=" + retryCount +
                ", commentId=" + (payload != null ? payload.getCommentId() : "null") +
                '}';
    }
}
