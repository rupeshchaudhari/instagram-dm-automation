package com.igdm.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class DashboardDto {

    public static class DashboardStatsResponse {
        private long totalDmsSent;
        private long dmsSentToday;
        private int activeRulesCount;
        private int connectedAccountsCount;
        private double conversionRate; // % of comments matched

        public DashboardStatsResponse(long totalDmsSent, long dmsSentToday, int activeRulesCount,
                                      int connectedAccountsCount, double conversionRate) {
            this.totalDmsSent = totalDmsSent;
            this.dmsSentToday = dmsSentToday;
            this.activeRulesCount = activeRulesCount;
            this.connectedAccountsCount = connectedAccountsCount;
            this.conversionRate = conversionRate;
        }

        public long getTotalDmsSent() { return totalDmsSent; }
        public long getDmsSentToday() { return dmsSentToday; }
        public int getActiveRulesCount() { return activeRulesCount; }
        public int getConnectedAccountsCount() { return connectedAccountsCount; }
        public double getConversionRate() { return conversionRate; }
    }

    public static class InteractionLogResponse {
        private UUID id;
        private String ruleName;
        private String igCommentId;
        private String igCommenterUsername;
        private String commentText;
        private String status;
        private String dmContentSent;
        private String errorMessage;
        private OffsetDateTime createdAt;

        public InteractionLogResponse(UUID id, String ruleName, String igCommentId, String igCommenterUsername,
                                      String commentText, String status, String dmContentSent,
                                      String errorMessage, OffsetDateTime createdAt) {
            this.id = id;
            this.ruleName = ruleName;
            this.igCommentId = igCommentId;
            this.igCommenterUsername = igCommenterUsername;
            this.commentText = commentText;
            this.status = status;
            this.dmContentSent = dmContentSent;
            this.errorMessage = errorMessage;
            this.createdAt = createdAt;
        }

        public UUID getId() { return id; }
        public String getRuleName() { return ruleName; }
        public String getIgCommentId() { return igCommentId; }
        public String getIgCommenterUsername() { return igCommenterUsername; }
        public String getCommentText() { return commentText; }
        public String getStatus() { return status; }
        public String getDmContentSent() { return dmContentSent; }
        public String getErrorMessage() { return errorMessage; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
    }
}
