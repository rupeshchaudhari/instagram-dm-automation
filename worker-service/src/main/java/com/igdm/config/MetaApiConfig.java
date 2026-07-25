package com.igdm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe configuration properties for Meta Graph API integration.
 * Bound from application.yml under the "app.meta" prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "app.meta")
public class MetaApiConfig {

    private String appId;
    private String appSecret;
    private String verifyToken;

    /**
     * Base URL for the Meta Graph API.
     * Defaults to v21.0 — update when Meta releases new stable versions.
     */
    private String graphApiBaseUrl = "https://graph.facebook.com/v21.0";

    /**
     * OAuth redirect URI for the Instagram login flow.
     * Must match what's registered in the Meta App Dashboard.
     */
    private String oauthRedirectUri = "http://localhost:8080/api/v1/instagram/callback";

    /**
     * Scopes requested during Instagram OAuth.
     * instagram_manage_messages — required for sending DMs
     * instagram_manage_comments — required for reading comments
     * pages_manage_metadata — required for page-linked IG accounts
     */
    private String oauthScopes = "instagram_manage_messages,instagram_manage_comments,pages_manage_metadata,pages_show_list";

    // --- Getters and Setters ---

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public String getVerifyToken() { return verifyToken; }
    public void setVerifyToken(String verifyToken) { this.verifyToken = verifyToken; }

    public String getGraphApiBaseUrl() { return graphApiBaseUrl; }
    public void setGraphApiBaseUrl(String graphApiBaseUrl) { this.graphApiBaseUrl = graphApiBaseUrl; }

    public String getOauthRedirectUri() { return oauthRedirectUri; }
    public void setOauthRedirectUri(String oauthRedirectUri) { this.oauthRedirectUri = oauthRedirectUri; }

    public String getOauthScopes() { return oauthScopes; }
    public void setOauthScopes(String oauthScopes) { this.oauthScopes = oauthScopes; }
}
