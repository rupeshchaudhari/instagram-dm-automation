package com.igdm.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AdminDto {

    public static class PlatformStatsResponse {
        private long totalUsers;
        private long totalConnectedAccounts;
        private long totalActiveRules;
        private long totalDmsSentPlatformWide;
        private RateLimitInfo rateLimit;

        public PlatformStatsResponse(long totalUsers, long totalConnectedAccounts, long totalActiveRules,
                                    long totalDmsSentPlatformWide, RateLimitInfo rateLimit) {
            this.totalUsers = totalUsers;
            this.totalConnectedAccounts = totalConnectedAccounts;
            this.totalActiveRules = totalActiveRules;
            this.totalDmsSentPlatformWide = totalDmsSentPlatformWide;
            this.rateLimit = rateLimit;
        }

        public long getTotalUsers() { return totalUsers; }
        public long getTotalConnectedAccounts() { return totalConnectedAccounts; }
        public long getTotalActiveRules() { return totalActiveRules; }
        public long getTotalDmsSentPlatformWide() { return totalDmsSentPlatformWide; }
        public RateLimitInfo getRateLimit() { return rateLimit; }
    }

    public static class RateLimitInfo {
        private int callCountPercent;
        private int cpuTimePercent;
        private int totalTimePercent;
        private boolean isThrottled;
        private Instant throttledUntil;

        public RateLimitInfo(int callCountPercent, int cpuTimePercent, int totalTimePercent,
                             boolean isThrottled, Instant throttledUntil) {
            this.callCountPercent = callCountPercent;
            this.cpuTimePercent = cpuTimePercent;
            this.totalTimePercent = totalTimePercent;
            this.isThrottled = isThrottled;
            this.throttledUntil = throttledUntil;
        }

        public int getCallCountPercent() { return callCountPercent; }
        public int getCpuTimePercent() { return cpuTimePercent; }
        public int getTotalTimePercent() { return totalTimePercent; }
        public boolean isThrottled() { return isThrottled; }
        public Instant getThrottledUntil() { return throttledUntil; }
    }

    public static class AdminUserItem {
        private UUID id;
        private String email;
        private String name;
        private String planTier;
        private boolean isActive;
        private int accountCount;
        private OffsetDateTime createdAt;

        public AdminUserItem(UUID id, String email, String name, String planTier,
                             boolean isActive, int accountCount, OffsetDateTime createdAt) {
            this.id = id;
            this.email = email;
            this.name = name;
            this.planTier = planTier;
            this.isActive = isActive;
            this.accountCount = accountCount;
            this.createdAt = createdAt;
        }

        public UUID getId() { return id; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getPlanTier() { return planTier; }
        public boolean isActive() { return isActive; }
        public int getAccountCount() { return accountCount; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
    }
}
