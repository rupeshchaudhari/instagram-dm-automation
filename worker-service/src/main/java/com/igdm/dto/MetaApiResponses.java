package com.igdm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTOs for Meta Graph API responses.
 */
public class MetaApiResponses {

    /**
     * Response from OAuth token exchange.
     * POST /oauth/access_token
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenExchangeResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Long expiresIn;

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }

        public Long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    }

    /**
     * Response from long-lived token exchange.
     * GET /oauth/access_token?grant_type=fb_exchange_token
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LongLivedTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Long expiresIn; // ~60 days in seconds

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }

        public Long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    }

    /**
     * Response from sending a DM.
     * POST /{ig-user-id}/messages
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SendMessageResponse {
        @JsonProperty("recipient_id")
        private String recipientId;

        @JsonProperty("message_id")
        private String messageId;

        public String getRecipientId() { return recipientId; }
        public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
    }

    /**
     * Instagram Business Account info from /me/accounts and /{page-id}?fields=instagram_business_account
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InstagramBusinessAccount {
        private String id;
        private String username;
        private String name;

        @JsonProperty("profile_picture_url")
        private String profilePictureUrl;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getProfilePictureUrl() { return profilePictureUrl; }
        public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    }

    /**
     * Error response from Meta Graph API.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphApiError {
        private ErrorDetail error;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ErrorDetail {
            private String message;
            private String type;
            private int code;

            @JsonProperty("error_subcode")
            private int errorSubcode;

            @JsonProperty("fbtrace_id")
            private String fbtraceId;

            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }

            public String getType() { return type; }
            public void setType(String type) { this.type = type; }

            public int getCode() { return code; }
            public void setCode(int code) { this.code = code; }

            public int getErrorSubcode() { return errorSubcode; }
            public void setErrorSubcode(int errorSubcode) { this.errorSubcode = errorSubcode; }

            public String getFbtraceId() { return fbtraceId; }
            public void setFbtraceId(String fbtraceId) { this.fbtraceId = fbtraceId; }
        }

        public ErrorDetail getError() { return error; }
        public void setError(ErrorDetail error) { this.error = error; }

        /**
         * Check if this is a rate-limit error (code 4 or 32).
         */
        public boolean isRateLimitError() {
            return error != null && (error.code == 4 || error.code == 32);
        }

        /**
         * Check if this is an expired/invalid token error (code 190).
         */
        public boolean isTokenError() {
            return error != null && error.code == 190;
        }
    }
}
