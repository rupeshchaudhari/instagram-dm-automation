package com.igdm.controller;

import com.igdm.dto.RuleDto.*;
import com.igdm.entity.AutomationRule;
import com.igdm.entity.InstagramAccount;
import com.igdm.entity.User;
import com.igdm.repository.AutomationRuleRepository;
import com.igdm.repository.InstagramAccountRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private static final Logger log = LoggerFactory.getLogger(RuleController.class);

    private final AutomationRuleRepository ruleRepository;
    private final InstagramAccountRepository igAccountRepository;

    public RuleController(
            AutomationRuleRepository ruleRepository,
            InstagramAccountRepository igAccountRepository) {
        this.ruleRepository = ruleRepository;
        this.igAccountRepository = igAccountRepository;
    }

    /**
     * GET /api/v1/rules — List all automation rules for authenticated user.
     */
    @GetMapping
    public ResponseEntity<?> getUserRules(@AuthenticationPrincipal User user) {
        List<InstagramAccount> accounts = igAccountRepository.findByUserIdAndIsConnectedTrue(user.getId());
        List<UUID> accountIds = accounts.stream().map(InstagramAccount::getId).toList();

        List<RuleResponse> rules = ruleRepository.findAll().stream()
                .filter(r -> accountIds.contains(r.getIgAccount().getId()))
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(Map.of("rules", rules));
    }

    /**
     * POST /api/v1/rules — Create a new automation rule.
     */
    @PostMapping
    public ResponseEntity<?> createRule(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateRuleRequest request) {

        InstagramAccount account;
        if (request.getIgAccountId() != null) {
            var accountOpt = igAccountRepository.findById(request.getIgAccountId());
            if (accountOpt.isEmpty() || !accountOpt.get().getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Instagram account not found"));
            }
            account = accountOpt.get();
        } else {
            // Pick first connected account
            var accounts = igAccountRepository.findByUserIdAndIsConnectedTrue(user.getId());
            if (accounts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No connected Instagram account found. Please connect an account first."));
            }
            account = accounts.get(0);
        }

        AutomationRule rule = new AutomationRule();
        rule.setIgAccount(account);
        rule.setName(request.getName());
        rule.setTriggerType(request.getTriggerType() != null ? request.getTriggerType() : "keyword");
        rule.setTriggerKeywords(request.getTriggerKeywords() != null ? request.getTriggerKeywords() : List.of());
        rule.setDmTemplate(request.getDmTemplate());
        rule.setDailyDmLimit(request.getDailyDmLimit() > 0 ? request.getDailyDmLimit() : 100);
        rule.setActive(true);

        rule = ruleRepository.save(rule);
        log.info("Created automation rule '{}' for user {}", rule.getName(), user.getEmail());

        return ResponseEntity.ok(mapToResponse(rule));
    }

    /**
     * GET /api/v1/rules/{id} — Get rule details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRule(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        var ruleOpt = ruleRepository.findById(id);
        if (ruleOpt.isEmpty() || !ruleOpt.get().getIgAccount().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToResponse(ruleOpt.get()));
    }

    /**
     * PUT /api/v1/rules/{id} — Update rule details.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRule(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody UpdateRuleRequest request) {

        var ruleOpt = ruleRepository.findById(id);
        if (ruleOpt.isEmpty() || !ruleOpt.get().getIgAccount().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        AutomationRule rule = ruleOpt.get();
        if (request.getName() != null) rule.setName(request.getName());
        if (request.getTriggerType() != null) rule.setTriggerType(request.getTriggerType());
        if (request.getTriggerKeywords() != null) rule.setTriggerKeywords(request.getTriggerKeywords());
        if (request.getDmTemplate() != null) rule.setDmTemplate(request.getDmTemplate());
        if (request.getDailyDmLimit() != null && request.getDailyDmLimit() > 0) {
            rule.setDailyDmLimit(request.getDailyDmLimit());
        }

        rule = ruleRepository.save(rule);
        return ResponseEntity.ok(mapToResponse(rule));
    }

    /**
     * PATCH /api/v1/rules/{id}/toggle — Enable or disable rule.
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleRule(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        var ruleOpt = ruleRepository.findById(id);
        if (ruleOpt.isEmpty() || !ruleOpt.get().getIgAccount().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        AutomationRule rule = ruleOpt.get();
        rule.setActive(!rule.isActive());
        rule = ruleRepository.save(rule);

        return ResponseEntity.ok(Map.of(
                "id", rule.getId(),
                "isActive", rule.isActive()
        ));
    }

    /**
     * DELETE /api/v1/rules/{id} — Delete rule.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRule(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        var ruleOpt = ruleRepository.findById(id);
        if (ruleOpt.isEmpty() || !ruleOpt.get().getIgAccount().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        ruleRepository.delete(ruleOpt.get());
        return ResponseEntity.ok(Map.of("message", "Rule deleted successfully"));
    }

    private RuleResponse mapToResponse(AutomationRule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getIgAccount().getId(),
                rule.getIgAccount().getIgUsername() != null ? rule.getIgAccount().getIgUsername() : "",
                rule.getName(),
                rule.getTriggerType(),
                rule.getTriggerKeywords(),
                rule.getDmTemplate(),
                rule.isActive(),
                rule.getDailyDmLimit(),
                rule.getDmSentToday(),
                rule.getCreatedAt()
        );
    }
}
