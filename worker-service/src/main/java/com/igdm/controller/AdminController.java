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
    public ResponseEntity<?> getPlatformStats() {
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
}
