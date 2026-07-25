package com.igdm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class RuleDto {

    public static class CreateRuleRequest {
        private UUID igAccountId;

        @NotBlank(message = "Rule name is required")
        private String name;

        private String triggerType = "keyword"; // keyword | any_comment | first_comment
        private List<String> triggerKeywords;

        @NotBlank(message = "DM template is required")
        private String dmTemplate;

        @Min(value = 1, message = "Daily DM limit must be at least 1")
        private int dailyDmLimit = 100;

        public UUID getIgAccountId() { return igAccountId; }
        public void setIgAccountId(UUID igAccountId) { this.igAccountId = igAccountId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

        public List<String> getTriggerKeywords() { return triggerKeywords; }
        public void setTriggerKeywords(List<String> triggerKeywords) { this.triggerKeywords = triggerKeywords; }

        public String getDmTemplate() { return dmTemplate; }
        public void setDmTemplate(String dmTemplate) { this.dmTemplate = dmTemplate; }

        public int getDailyDmLimit() { return dailyDmLimit; }
        public void setDailyDmLimit(int dailyDmLimit) { this.dailyDmLimit = dailyDmLimit; }
    }

    public static class UpdateRuleRequest {
        private String name;
        private String triggerType;
        private List<String> triggerKeywords;
        private String dmTemplate;
        private Integer dailyDmLimit;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

        public List<String> getTriggerKeywords() { return triggerKeywords; }
        public void setTriggerKeywords(List<String> triggerKeywords) { this.triggerKeywords = triggerKeywords; }

        public String getDmTemplate() { return dmTemplate; }
        public void setDmTemplate(String dmTemplate) { this.dmTemplate = dmTemplate; }

        public Integer getDailyDmLimit() { return dailyDmLimit; }
        public void setDailyDmLimit(Integer dailyDmLimit) { this.dailyDmLimit = dailyDmLimit; }
    }

    public static class RuleResponse {
        private UUID id;
        private UUID igAccountId;
        private String igUsername;
        private String name;
        private String triggerType;
        private List<String> triggerKeywords;
        private String dmTemplate;
        private boolean isActive;
        private int dailyDmLimit;
        private int dmSentToday;
        private OffsetDateTime createdAt;

        public RuleResponse(UUID id, UUID igAccountId, String igUsername, String name,
                            String triggerType, List<String> triggerKeywords, String dmTemplate,
                            boolean isActive, int dailyDmLimit, int dmSentToday, OffsetDateTime createdAt) {
            this.id = id;
            this.igAccountId = igAccountId;
            this.igUsername = igUsername;
            this.name = name;
            this.triggerType = triggerType;
            this.triggerKeywords = triggerKeywords;
            this.dmTemplate = dmTemplate;
            this.isActive = isActive;
            this.dailyDmLimit = dailyDmLimit;
            this.dmSentToday = dmSentToday;
            this.createdAt = createdAt;
        }

        public UUID getId() { return id; }
        public UUID getIgAccountId() { return igAccountId; }
        public String getIgUsername() { return igUsername; }
        public String getName() { return name; }
        public String getTriggerType() { return triggerType; }
        public List<String> getTriggerKeywords() { return triggerKeywords; }
        public String getDmTemplate() { return dmTemplate; }
        public boolean isActive() { return isActive; }
        public int getDailyDmLimit() { return dailyDmLimit; }
        public int getDmSentToday() { return dmSentToday; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
    }
}
