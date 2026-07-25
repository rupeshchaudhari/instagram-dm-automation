package com.igdm.scheduler;

import com.igdm.repository.AutomationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled tasks for daily maintenance operations.
 */
@Component
public class DailyResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyResetScheduler.class);

    private final AutomationRuleRepository ruleRepo;

    public DailyResetScheduler(AutomationRuleRepository ruleRepo) {
        this.ruleRepo = ruleRepo;
    }

    /**
     * Reset all daily DM counters at midnight UTC.
     * Cron: 0 0 0 * * * → every day at 00:00:00 UTC
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void resetDailyDmCounters() {
        int updated = ruleRepo.resetAllDailyCounters();
        log.info("Daily DM counters reset for {} rules", updated);
    }
}
