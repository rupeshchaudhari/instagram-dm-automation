package com.igdm.controller;

import com.igdm.dto.AdminDto.*;
import com.igdm.entity.AutomationRule;
import com.igdm.entity.User;
import com.igdm.repository.AutomationRuleRepository;
import com.igdm.repository.InstagramAccountRepository;
import com.igdm.repository.InteractionLogRepository;
import com.igdm.repository.UserRepository;
import com.igdm.service.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;
    private final InstagramAccountRepository igAccountRepository;
    private final AutomationRuleRepository ruleRepository;
    private final InteractionLogRepository logRepository;
    private final RateLimitService rateLimitService;

    public AdminController(
            UserRepository userRepository,
            InstagramAccountRepository igAccountRepository,
            AutomationRuleRepository ruleRepository,
            InteractionLogRepository logRepository,
            RateLimitService rateLimitService) {
        this.userRepository = userRepository;
        this.igAccountRepository = igAccountRepository;
        this.ruleRepository = ruleRepository;
        this.logRepository = logRepository;
        this.rateLimitService = rateLimitService;
    }

    /**
     * GET /api/v1/admin/stats — System-wide platform metrics & Meta rate limit meter.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getPlatformStats(@org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        if (user != null && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Super Admin privileges required."));
        }

        long totalUsers = userRepository.count();
        long totalConnectedAccounts = igAccountRepository.findAll().stream().filter(a -> a.isConnected()).count();
        long totalActiveRules = ruleRepository.findAll().stream().filter(r -> r.isActive()).count();
        long totalSent = logRepository.findAll().stream().filter(l -> "SENT".equals(l.getStatus())).count();

        var rlState = rateLimitService.getCurrentState();
        RateLimitInfo rateLimit = new RateLimitInfo(
                rlState.callCountPercent(),
                rlState.cpuTimePercent(),
                rlState.totalTimePercent(),
                rlState.isThrottled(),
                rlState.throttledUntil()
        );

        PlatformStatsResponse response = new PlatformStatsResponse(
                totalUsers,
                totalConnectedAccounts,
                totalActiveRules,
                totalSent,
                rateLimit
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/admin/users — List all registered users in the platform.
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<AdminUserItem> users = userRepository.findAll().stream()
                .map(u -> {
                    int accountsCount = igAccountRepository.findByUserIdAndIsConnectedTrue(u.getId()).size();
                    return new AdminUserItem(
                            u.getId(),
                            u.getEmail(),
                            u.getName() != null ? u.getName() : "User",
                            u.getPlanTier(),
                            u.isActive(),
                            accountsCount,
                            u.getCreatedAt()
                    );
                })
                .toList();

        return ResponseEntity.ok(Map.of("users", users));
    }

    /**
     * PATCH /api/v1/admin/users/{id}/plan — Change user plan tier (free | pro | enterprise).
     */
    @PatchMapping("/users/{id}/plan")
    public ResponseEntity<?> updateUserPlan(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        String newPlan = body.get("planTier");
        if (newPlan == null || (!newPlan.equals("free") && !newPlan.equals("pro") && !newPlan.equals("enterprise"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid plan tier. Must be free, pro, or enterprise"));
        }

        var userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setPlanTier(newPlan);
        userRepository.save(user);

        log.info("Admin updated plan tier for user {} to {}", user.getEmail(), newPlan);
        return ResponseEntity.ok(Map.of("message", "User plan updated", "planTier", newPlan));
    }

    /**
     * PATCH /api/v1/admin/users/{id}/toggle — Suspend or reactivate tenant user.
     */
    @PatchMapping("/users/{id}/toggle")
    public ResponseEntity<?> toggleUserStatus(@PathVariable UUID id) {
        var userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setActive(!user.isActive());
        userRepository.save(user);

        log.info("Admin toggled active status for user {} to {}", user.getEmail(), user.isActive());
        return ResponseEntity.ok(Map.of("id", user.getId(), "isActive", user.isActive()));
    }

    /**
     * POST /api/v1/admin/seed — Pre-populates sample rules and realistic interaction logs for all users.
     */
    @PostMapping("/seed")
    public ResponseEntity<?> seedDemoData() {
        var users = userRepository.findAll();
        if (users.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No users found in database. Please register a user first."));
        }

        int totalLogsSeeded = 0;
        int totalRulesCreated = 0;

        for (User user : users) {
            // 1. Ensure user has an Instagram account
            var accounts = igAccountRepository.findByUserIdAndIsConnectedTrue(user.getId());
            com.igdm.entity.InstagramAccount account;
            if (accounts.isEmpty()) {
                account = new com.igdm.entity.InstagramAccount();
                account.setUser(user);
                account.setIgUserId("17841400" + Math.abs(user.getId().hashCode()));
                account.setIgUsername(user.getEmail().split("@")[0] + "_ig");
                account.setPageId("PAGE_" + Math.abs(user.getId().hashCode()));
                account.setAccessTokenEncrypted("encrypted_demo_token");
                account.setConnected(true);
                account = igAccountRepository.save(account);
            } else {
                account = accounts.get(0);
            }

            // 2. Create sample rules if user has none
            var rules = ruleRepository.findByIgAccountIdAndIsActiveTrue(account.getId());
            if (rules.isEmpty()) {
                AutomationRule rule1 = new AutomationRule();
                rule1.setIgAccount(account);
                rule1.setName("Summer Sale Promo");
                rule1.setTriggerType("keyword");
                rule1.setTriggerKeywords(List.of("link", "discount", "sale"));
                rule1.setDmTemplate("Hey {{username}}! Here is your exclusive 20% discount link: https://example.com/summer-sale");
                rule1.setDailyDmLimit(200);
                rule1.setActive(true);

                AutomationRule rule2 = new AutomationRule();
                rule2.setIgAccount(account);
                rule2.setName("Free VIP Guide Download");
                rule2.setTriggerType("keyword");
                rule2.setTriggerKeywords(List.of("guide", "vip", "ebook"));
                rule2.setDmTemplate("Hi {{username}}! Grab your free VIP guide here: https://example.com/vip-guide");
                rule2.setDailyDmLimit(150);
                rule2.setActive(true);

                AutomationRule rule3 = new AutomationRule();
                rule3.setIgAccount(account);
                rule3.setName("Pricing Info Request");
                rule3.setTriggerType("keyword");
                rule3.setTriggerKeywords(List.of("price", "cost", "info"));
                rule3.setDmTemplate("Hey {{username}}! Check out our pricing packages here: https://example.com/pricing");
                rule3.setDailyDmLimit(300);
                rule3.setActive(true);

                ruleRepository.saveAll(List.of(rule1, rule2, rule3));
                rules = List.of(rule1, rule2, rule3);
                totalRulesCreated += 3;
            }

            AutomationRule primaryRule = rules.get(0);

            // 3. Seed realistic interaction logs
            String[] usernames = {"sarah_growth", "mike_creator", "dev_alex", "emma_design", "tech_john", "lisa_marketing", "sam_startup"};
            String[] comments = {"Send me the link please!", "Discount code?", "Can I get the ebook?", "How much does it cost?", "Link info!", "Awesome, send link", "Price detail pls"};
            String[] statuses = {"SENT", "SENT", "SENT", "SENT", "SKIPPED", "RATE_LIMITED", "FAILED"};

            for (int i = 0; i < 25; i++) {
                String username = usernames[i % usernames.length];
                String comment = comments[i % comments.length];
                String status = statuses[i % statuses.length];
                String commentId = "COMMENT_SEED_" + System.currentTimeMillis() + "_" + user.getId().toString().substring(0, 4) + "_" + i;
                String dmContent = primaryRule.getDmTemplate().replace("{{username}}", username);
                String errorMsg = "FAILED".equals(status) ? "Meta Graph API 400: User privacy settings restrict DMs" : null;

                com.igdm.entity.InteractionLog logEntry = new com.igdm.entity.InteractionLog();
                logEntry.setRule(primaryRule);
                logEntry.setIgCommentId(commentId);
                logEntry.setIgCommenterId("178414009876543" + i);
                logEntry.setIgCommenterUsername(username);
                logEntry.setCommentText(comment);
                logEntry.setStatus(status);
                logEntry.setDmContentSent(dmContent);
                logEntry.setErrorMessage(errorMsg);

                logRepository.save(logEntry);
                totalLogsSeeded++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Demo data seeded successfully for all users!",
                "rulesCreated", totalRulesCreated,
                "logsSeeded", totalLogsSeeded,
                "usersSeeded", users.size()
        ));
    }
}
