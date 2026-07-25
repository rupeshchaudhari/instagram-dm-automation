package com.igdm.repository;

import com.igdm.entity.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID> {

    /**
     * Find all active rules for a given Instagram account.
     * This is the primary query in the hot path — called for every incoming comment.
     */
    List<AutomationRule> findByIgAccountIdAndIsActiveTrue(UUID igAccountId);

    /**
     * Find all rules belonging to an Instagram account (active and inactive).
     */
    List<AutomationRule> findByIgAccountId(UUID igAccountId);

    /**
     * Reset daily DM counters for all rules.
     * Called by a scheduled cron job at midnight UTC.
     */
    @Modifying
    @Query("UPDATE AutomationRule r SET r.dmSentToday = 0")
    int resetAllDailyCounters();

    /**
     * Increment the daily DM counter for a specific rule.
     * Uses a native query for atomic increment (avoids race conditions).
     */
    @Modifying
    @Query(value = "UPDATE automation_rules SET dm_sent_today = dm_sent_today + 1 WHERE id = :ruleId",
           nativeQuery = true)
    int incrementDmSentToday(UUID ruleId);
}
