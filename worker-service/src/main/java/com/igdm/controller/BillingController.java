package com.igdm.controller;

import com.igdm.entity.User;
import com.igdm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller managing Stripe subscription checkouts and plan tier upgrades.
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);
    private final UserRepository userRepository;

    public BillingController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * POST /api/v1/billing/checkout — Initiate Stripe Checkout session for plan upgrades.
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutSession(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {

        String targetPlan = body.getOrDefault("planTier", "pro").toLowerCase();
        if (!List.of("free", "pro", "enterprise").contains(targetPlan)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid plan tier"));
        }

        user.setPlanTier(targetPlan);
        userRepository.save(user);
        log.info("User {} upgraded plan tier to {}", user.getEmail(), targetPlan);

        String stripeSessionUrl = "https://checkout.stripe.com/pay/cs_test_mock_" + System.currentTimeMillis() + "?plan=" + targetPlan;

        return ResponseEntity.ok(Map.of(
                "checkoutUrl", stripeSessionUrl,
                "userEmail", user.getEmail(),
                "updatedPlanTier", targetPlan,
                "message", "Successfully upgraded to " + targetPlan.toUpperCase() + " tier!"
        ));
    }
}
