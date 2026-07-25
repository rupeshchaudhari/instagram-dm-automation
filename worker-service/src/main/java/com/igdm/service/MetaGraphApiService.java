package com.igdm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igdm.config.MetaApiConfig;
import com.igdm.dto.MetaApiResponses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with the Meta Graph API.
 *
 * Handles:
 * 1. OAuth token exchange (short-lived → long-lived)
 * 2. Fetching Instagram Business Account info
 * 3. Sending Direct Messages to Instagram users
 * 4. 24-hour messaging window enforcement
 * 5. Rate limit header inspection (x-app-usage)
 *
 * @see <a href="https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login/messaging">
 *      Meta Instagram Messaging API</a>
 */
@Service
public class MetaGraphApiService {

    private static final Logger log = LoggerFactory.getLogger(MetaGraphApiService.class);

    private final MetaApiConfig metaConfig;
    private final RateLimitService rateLimitService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MetaGraphApiService(
            MetaApiConfig metaConfig,
            RateLimitService rateLimitService,
            ObjectMapper objectMapper) {
        this.metaConfig = metaConfig;
        this.rateLimitService = rateLimitService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    // ================================================================
    // 1. OAuth Token Exchange
    // ================================================================

    /**
     * Exchange an OAuth authorization code for a short-lived token.
     *
     * @param code The authorization code from the OAuth callback
     * @return TokenExchangeResponse containing the short-lived token
     */
    public TokenExchangeResponse exchangeCodeForToken(String code) {
        String url = UriComponentsBuilder
                .fromHttpUrl(metaConfig.getGraphApiBaseUrl() + "/oauth/access_token")
                .queryParam("client_id", metaConfig.getAppId())
                .queryParam("client_secret", metaConfig.getAppSecret())
                .queryParam("redirect_uri", metaConfig.getOauthRedirectUri())
                .queryParam("code", code)
                .toUriString();

        log.info("Exchanging OAuth code for token");

        try {
            ResponseEntity<TokenExchangeResponse> response = restTemplate.getForEntity(
                    url, TokenExchangeResponse.class);
            inspectRateLimitHeaders(response.getHeaders());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Token exchange failed: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to exchange OAuth code: " + e.getMessage(), e);
        }
    }

    /**
     * Exchange a short-lived token for a long-lived token (~60 days).
     *
     * @param shortLivedToken The short-lived token to exchange
     * @return LongLivedTokenResponse containing the long-lived token
     */
    public LongLivedTokenResponse exchangeForLongLivedToken(String shortLivedToken) {
        String url = UriComponentsBuilder
                .fromHttpUrl(metaConfig.getGraphApiBaseUrl() + "/oauth/access_token")
                .queryParam("grant_type", "fb_exchange_token")
                .queryParam("client_id", metaConfig.getAppId())
                .queryParam("client_secret", metaConfig.getAppSecret())
                .queryParam("fb_exchange_token", shortLivedToken)
                .toUriString();

        log.info("Exchanging short-lived token for long-lived token");

        try {
            ResponseEntity<LongLivedTokenResponse> response = restTemplate.getForEntity(
                    url, LongLivedTokenResponse.class);
            inspectRateLimitHeaders(response.getHeaders());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Long-lived token exchange failed: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to exchange for long-lived token: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // 2. Instagram Account Discovery
    // ================================================================

    /**
     * Get the Instagram Business Account linked to a Facebook Page.
     * Flow: access_token → /me/accounts → page_id → /{page_id}?fields=instagram_business_account
     *
     * @param accessToken The user's long-lived access token
     * @return Map with keys: igUserId, igUsername, pageId
     */
    public Map<String, String> discoverInstagramAccount(String accessToken) {
        // Step 1: Get Pages
        String pagesUrl = UriComponentsBuilder
                .fromHttpUrl(metaConfig.getGraphApiBaseUrl() + "/me/accounts")
                .queryParam("access_token", accessToken)
                .queryParam("fields", "id,name,instagram_business_account{id,username}")
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(pagesUrl, String.class);
            inspectRateLimitHeaders(response.getHeaders());

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");

            if (data == null || !data.isArray() || data.isEmpty()) {
                throw new RuntimeException("No Facebook Pages found for this account");
            }

            // Find the first page with an Instagram Business Account
            for (JsonNode page : data) {
                JsonNode igAccount = page.get("instagram_business_account");
                if (igAccount != null) {
                    Map<String, String> result = new HashMap<>();
                    result.put("pageId", page.get("id").asText());
                    result.put("igUserId", igAccount.get("id").asText());

                    // Get username
                    if (igAccount.has("username")) {
                        result.put("igUsername", igAccount.get("username").asText());
                    }

                    log.info("Discovered Instagram account: igUserId={}, username={}",
                            result.get("igUserId"), result.get("igUsername"));
                    return result;
                }
            }

            throw new RuntimeException("No Instagram Business Account linked to any Facebook Page");

        } catch (HttpClientErrorException e) {
            log.error("Instagram account discovery failed: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to discover Instagram account: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Instagram account response", e);
        }
    }

    // ================================================================
    // 3. Send Direct Message
    // ================================================================

    /**
     * Result of a DM send attempt, including rate limit state.
     */
    public record SendDmResult(
            boolean success,
            String messageId,
            String errorMessage,
            boolean isRateLimited,
            boolean isTokenError,
            boolean shouldRetry
    ) {
        public static SendDmResult success(String messageId) {
            return new SendDmResult(true, messageId, null, false, false, false);
        }

        public static SendDmResult rateLimited() {
            return new SendDmResult(false, null, "Rate limited by Meta API",
                    true, false, true);
        }

        public static SendDmResult tokenError(String message) {
            return new SendDmResult(false, null, message, false, true, false);
        }

        public static SendDmResult failed(String message, boolean shouldRetry) {
            return new SendDmResult(false, null, message, false, false, shouldRetry);
        }
    }

    /**
     * Send a Direct Message to an Instagram user.
     *
     * Uses the Instagram Messaging API:
     * POST /{ig-user-id}/messages
     * {
     *   "recipient": { "id": "<IGSID>" },
     *   "message": { "text": "Hello!" }
     * }
     *
     * IMPORTANT: Messages can only be sent within the 24-hour messaging window.
     * A user must have interacted with the IG account (comment, DM, etc.) within
     * the last 24 hours for a message to be delivered.
     *
     * @param igAccountId  The IG Business Account ID (sender)
     * @param recipientId  The Instagram-Scoped ID of the commenter (recipient)
     * @param messageText  The DM content to send
     * @param accessToken  The decrypted long-lived access token
     * @param commentTimestamp  When the comment was made (for 24h window check)
     * @return SendDmResult with success/failure details
     */
    public SendDmResult sendDirectMessage(
            String igAccountId,
            String recipientId,
            String messageText,
            String accessToken,
            Instant commentTimestamp) {

        // --- Check 24-hour messaging window ---
        if (commentTimestamp != null) {
            Instant windowDeadline = commentTimestamp.plusSeconds(24 * 60 * 60);
            if (Instant.now().isAfter(windowDeadline)) {
                log.warn("24-hour messaging window expired for comment. " +
                                "Comment at: {}, deadline was: {}",
                        commentTimestamp, windowDeadline);
                return SendDmResult.failed("24-hour messaging window expired", false);
            }
        }

        // --- Check rate limit state before making the call ---
        if (rateLimitService.isThrottled()) {
            log.warn("Rate limit threshold exceeded — deferring DM send");
            return SendDmResult.rateLimited();
        }

        // --- Build request ---
        String url = metaConfig.getGraphApiBaseUrl() + "/" + igAccountId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
                "recipient", Map.of("id", recipientId),
                "message", Map.of("text", messageText)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Sending DM: igAccountId={}, recipientId={}, textLength={}",
                    igAccountId, recipientId, messageText.length());

            ResponseEntity<SendMessageResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, SendMessageResponse.class);

            // Inspect rate limit headers from the response
            inspectRateLimitHeaders(response.getHeaders());

            SendMessageResponse responseBody = response.getBody();
            if (responseBody != null && responseBody.getMessageId() != null) {
                log.info("DM sent successfully: messageId={}", responseBody.getMessageId());
                return SendDmResult.success(responseBody.getMessageId());
            }

            return SendDmResult.failed("Empty response from Meta API", true);

        } catch (HttpClientErrorException e) {
            return handleApiError(e);
        } catch (Exception e) {
            log.error("Unexpected error sending DM: {}", e.getMessage(), e);
            return SendDmResult.failed("Unexpected error: " + e.getMessage(), true);
        }
    }

    // ================================================================
    // 4. Rate Limit Header Inspection
    // ================================================================

    /**
     * Inspect x-app-usage and x-business-use-case-usage headers from Meta API responses.
     *
     * x-app-usage header format:
     * {
     *   "call_count": 28,           // % of calls used in the rolling window
     *   "total_cputime": 5,         // % of CPU time used
     *   "total_time": 12            // % of total time used
     * }
     *
     * When any value exceeds 100%, the app is rate-limited (HTTP 429).
     * We proactively throttle at 80% to avoid hitting the hard limit.
     */
    void inspectRateLimitHeaders(HttpHeaders headers) {
        String appUsage = headers.getFirst("x-app-usage");
        if (appUsage != null) {
            rateLimitService.updateFromAppUsageHeader(appUsage);
        }

        String businessUsage = headers.getFirst("x-business-use-case-usage");
        if (businessUsage != null) {
            rateLimitService.updateFromBusinessUsageHeader(businessUsage);
        }
    }

    // ================================================================
    // 5. Error Handling
    // ================================================================

    /**
     * Parse Meta Graph API error responses and determine retry strategy.
     */
    private SendDmResult handleApiError(HttpClientErrorException e) {
        try {
            GraphApiError error = objectMapper.readValue(
                    e.getResponseBodyAsString(), GraphApiError.class);

            if (error.isRateLimitError()) {
                log.warn("Meta API rate limit hit: {}", error.getError().getMessage());
                rateLimitService.markRateLimited();
                return SendDmResult.rateLimited();
            }

            if (error.isTokenError()) {
                log.error("Meta API token error: {}", error.getError().getMessage());
                return SendDmResult.tokenError(error.getError().getMessage());
            }

            String errorMsg = error.getError() != null
                    ? error.getError().getMessage()
                    : e.getResponseBodyAsString();

            // 4xx errors (except rate limit) are generally not retryable
            boolean shouldRetry = e.getStatusCode().is5xxServerError();
            log.error("Meta API error (code={}): {}", error.getError().getCode(), errorMsg);
            return SendDmResult.failed(errorMsg, shouldRetry);

        } catch (Exception parseError) {
            log.error("Failed to parse Meta API error response: {}", e.getResponseBodyAsString());
            return SendDmResult.failed(e.getResponseBodyAsString(), true);
        }
    }

    /**
     * Build the OAuth authorization URL for the Instagram login flow.
     * The frontend redirects the user to this URL to start OAuth.
     */
    public String buildOAuthUrl(String state) {
        return UriComponentsBuilder
                .fromHttpUrl("https://www.facebook.com/v21.0/dialog/oauth")
                .queryParam("client_id", metaConfig.getAppId())
                .queryParam("redirect_uri", metaConfig.getOauthRedirectUri())
                .queryParam("scope", metaConfig.getOauthScopes())
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .toUriString();
    }
}
