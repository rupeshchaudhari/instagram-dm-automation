package com.igdm.controller;

import com.igdm.dto.MetaApiResponses.*;
import com.igdm.entity.InstagramAccount;
import com.igdm.repository.InstagramAccountRepository;
import com.igdm.service.MetaGraphApiService;
import com.igdm.service.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Handles Instagram OAuth flow and account management.
 *
 * OAuth Flow:
 * 1. Frontend calls GET /api/v1/instagram/connect → receives redirect URL
 * 2. User authorizes on Facebook/Instagram → redirected to callback
 * 3. GET /api/v1/instagram/callback → exchanges code for tokens, discovers IG account
 * 4. Stores encrypted long-lived token and IG account metadata
 */
@RestController
@RequestMapping("/api/v1/instagram")
public class InstagramController {

    private static final Logger log = LoggerFactory.getLogger(InstagramController.class);

    private final MetaGraphApiService metaApiService;
    private final TokenEncryptionService encryptionService;
    private final InstagramAccountRepository igAccountRepo;

    public InstagramController(
            MetaGraphApiService metaApiService,
            TokenEncryptionService encryptionService,
            InstagramAccountRepository igAccountRepo) {
        this.metaApiService = metaApiService;
        this.encryptionService = encryptionService;
        this.igAccountRepo = igAccountRepo;
    }

    /**
     * Step 1: Initiate the Instagram OAuth flow.
     * Returns the Meta OAuth URL that the frontend should redirect the user to.
     */
    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> connect() {
        // Generate a random state parameter to prevent CSRF
        String state = UUID.randomUUID().toString();

        String oauthUrl = metaApiService.buildOAuthUrl(state);

        log.info("OAuth flow initiated, state={}", state);

        return ResponseEntity.ok(Map.of(
                "url", oauthUrl,
                "state", state
        ));
    }

    /**
     * Step 2: OAuth callback — exchange code for tokens and store account.
     *
     * Flow:
     * 1. Exchange authorization code → short-lived token
     * 2. Exchange short-lived → long-lived token (~60 days)
     * 3. Discover Instagram Business Account via Page API
     * 4. Encrypt and store the token + account metadata
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {

        log.info("OAuth callback received, state={}", state);

        try {
            // 1. Exchange code for short-lived token
            TokenExchangeResponse tokenResponse = metaApiService.exchangeCodeForToken(code);
            String shortLivedToken = tokenResponse.getAccessToken();

            // 2. Exchange for long-lived token
            LongLivedTokenResponse longLivedResponse = metaApiService.exchangeForLongLivedToken(shortLivedToken);
            String longLivedToken = longLivedResponse.getAccessToken();
            long expiresInSeconds = longLivedResponse.getExpiresIn() != null
                    ? longLivedResponse.getExpiresIn()
                    : 5184000L; // Default 60 days

            // 3. Discover Instagram Business Account
            Map<String, String> igInfo = metaApiService.discoverInstagramAccount(longLivedToken);
            String igUserId = igInfo.get("igUserId");
            String igUsername = igInfo.get("igUsername");
            String pageId = igInfo.get("pageId");

            // 4. Save or update the Instagram account
            InstagramAccount account = igAccountRepo.findByIgUserId(igUserId)
                    .orElseGet(InstagramAccount::new);

            account.setIgUserId(igUserId);
            account.setIgUsername(igUsername);
            account.setPageId(pageId);
            account.setAccessTokenEncrypted(encryptionService.encrypt(longLivedToken));
            account.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(expiresInSeconds));
            account.setConnected(true);

            // TODO: Set user from authenticated session (JWT) — for now, user association
            // will be handled when we implement full JWT auth

            igAccountRepo.save(account);

            log.info("Instagram account connected: igUserId={}, username={}", igUserId, igUsername);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "igUserId", igUserId,
                    "igUsername", igUsername != null ? igUsername : "",
                    "message", "Instagram account connected successfully"
            ));

        } catch (Exception e) {
            log.error("OAuth callback failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * List all connected Instagram accounts.
     * TODO: Filter by authenticated user once JWT auth is implemented.
     */
    @GetMapping("/accounts")
    public ResponseEntity<?> listAccounts() {
        var accounts = igAccountRepo.findAll().stream()
                .filter(InstagramAccount::isConnected)
                .map(account -> Map.of(
                        "id", account.getId().toString(),
                        "igUserId", account.getIgUserId(),
                        "igUsername", account.getIgUsername() != null ? account.getIgUsername() : "",
                        "isConnected", account.isConnected(),
                        "tokenExpiresAt", account.getTokenExpiresAt() != null
                                ? account.getTokenExpiresAt().toString() : ""
                ))
                .toList();

        return ResponseEntity.ok(Map.of("accounts", accounts));
    }

    /**
     * Disconnect an Instagram account.
     */
    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Map<String, Object>> disconnect(@PathVariable UUID id) {
        return igAccountRepo.findById(id)
                .map(account -> {
                    account.setConnected(false);
                    igAccountRepo.save(account);
                    log.info("Instagram account disconnected: {}", account.getIgUserId());
                    return ResponseEntity.ok(Map.of(
                            "success", (Object) true,
                            "message", (Object) "Account disconnected"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
