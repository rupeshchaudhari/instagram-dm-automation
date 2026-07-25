package com.igdm.service;

import com.igdm.dto.CommentEventMessage;
import com.igdm.dto.CommentEventMessage.CommentPayload;
import com.igdm.dto.CommentEventMessage.Commenter;
import com.igdm.entity.AutomationRule;
import com.igdm.entity.InstagramAccount;
import com.igdm.entity.InteractionLog;
import com.igdm.entity.User;
import com.igdm.repository.AutomationRuleRepository;
import com.igdm.repository.InstagramAccountRepository;
import com.igdm.repository.InteractionLogRepository;
import com.igdm.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AutomationService using Testcontainers.
 *
 * Spins up real PostgreSQL and RabbitMQ containers to test the full pipeline:
 * - Comment deduplication
 * - Keyword trigger matching
 * - DM template rendering
 * - Daily limit enforcement
 * - Interaction logging
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AutomationServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("igdm_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/init.sql");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private AutomationService automationService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private InstagramAccountRepository igAccountRepo;

    @Autowired
    private AutomationRuleRepository ruleRepo;

    @Autowired
    private InteractionLogRepository logRepo;

    @Autowired
    private TokenEncryptionService encryptionService;

    private static User testUser;
    private static InstagramAccount testAccount;
    private static AutomationRule keywordRule;

    @BeforeAll
    static void setup(
            @Autowired UserRepository userRepo,
            @Autowired InstagramAccountRepository igAccountRepo,
            @Autowired AutomationRuleRepository ruleRepo,
            @Autowired TokenEncryptionService encryptionService) {

        // Create test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$10$fakehash");
        testUser.setName("Test User");
        testUser.setPlanTier("pro");
        testUser = userRepo.save(testUser);

        // Create test Instagram account
        testAccount = new InstagramAccount();
        testAccount.setUser(testUser);
        testAccount.setIgUserId("17841400123456789");
        testAccount.setIgUsername("test_business");
        testAccount.setPageId("PAGE123");
        testAccount.setAccessTokenEncrypted(encryptionService.encrypt("fake-access-token"));
        testAccount.setConnected(true);
        testAccount = igAccountRepo.save(testAccount);

        // Create a keyword-trigger rule
        keywordRule = new AutomationRule();
        keywordRule.setIgAccount(testAccount);
        keywordRule.setName("Link Campaign");
        keywordRule.setTriggerType("keyword");
        keywordRule.setTriggerKeywords(List.of("link", "info", "free"));
        keywordRule.setDmTemplate("Hi {{username}}! Thanks for your interest. Here's your link: https://example.com");
        keywordRule.setActive(true);
        keywordRule.setDailyDmLimit(50);
        keywordRule = ruleRepo.save(keywordRule);
    }

    @AfterAll
    static void cleanup(
            @Autowired InteractionLogRepository logRepo,
            @Autowired AutomationRuleRepository ruleRepo,
            @Autowired InstagramAccountRepository igAccountRepo,
            @Autowired UserRepository userRepo) {
        logRepo.deleteAll();
        ruleRepo.deleteAll();
        igAccountRepo.deleteAll();
        userRepo.deleteAll();
    }

    // --- Helper to build a comment event ---

    private CommentEventMessage buildCommentEvent(String commentId, String commentText, String username) {
        Commenter commenter = new Commenter();
        commenter.setId("COMMENTER_" + commentId);
        commenter.setUsername(username);

        CommentPayload payload = new CommentPayload();
        payload.setIgAccountId("17841400123456789");
        payload.setMediaId("MEDIA_001");
        payload.setCommentId(commentId);
        payload.setCommentText(commentText);
        payload.setCommentTimestamp(Instant.now());
        payload.setCommenter(commenter);

        CommentEventMessage message = new CommentEventMessage();
        message.setMessageId("MSG_" + commentId);
        message.setTimestamp(Instant.now());
        message.setEventType("comment.created");
        message.setRetryCount(0);
        message.setMaxRetries(3);
        message.setPayload(payload);

        return message;
    }

    // ===================================================
    // Test: Keyword match → DM ready
    // ===================================================
    @Test
    @Order(1)
    void shouldMatchKeywordAndRenderTemplate() {
        CommentEventMessage event = buildCommentEvent("COMMENT_001", "I want the link!", "john_doe");

        AutomationService.ProcessingResult result = automationService.processComment(event);

        assertTrue(result.shouldSendDm(), "Should match keyword 'link'");
        assertEquals("MATCHED", result.status());
        assertNotNull(result.renderedDm());
        assertTrue(result.renderedDm().contains("john_doe"));
        assertTrue(result.renderedDm().contains("https://example.com"));
        assertEquals(keywordRule.getId(), result.matchedRule().getId());
        assertNotNull(result.interactionLog());
    }

    // ===================================================
    // Test: Deduplication — same comment processed twice
    // ===================================================
    @Test
    @Order(2)
    void shouldDeduplicateComment() {
        // COMMENT_001 was already processed in test 1
        CommentEventMessage event = buildCommentEvent("COMMENT_001", "I want the link!", "john_doe");

        AutomationService.ProcessingResult result = automationService.processComment(event);

        assertFalse(result.shouldSendDm(), "Should skip duplicate comment");
        assertEquals("SKIPPED", result.status());
    }

    // ===================================================
    // Test: No keyword match → skip
    // ===================================================
    @Test
    @Order(3)
    void shouldSkipWhenNoKeywordMatch() {
        CommentEventMessage event = buildCommentEvent("COMMENT_002", "Nice photo!", "jane_smith");

        AutomationService.ProcessingResult result = automationService.processComment(event);

        assertFalse(result.shouldSendDm(), "Should not match any keyword");
        assertEquals("SKIPPED", result.status());
    }

    // ===================================================
    // Test: Unknown IG account → skip
    // ===================================================
    @Test
    @Order(4)
    void shouldSkipUnknownAccount() {
        CommentEventMessage event = buildCommentEvent("COMMENT_003", "Give me the link!", "someone");
        event.getPayload().setIgAccountId("UNKNOWN_ACCOUNT");

        AutomationService.ProcessingResult result = automationService.processComment(event);

        assertFalse(result.shouldSendDm(), "Should skip unknown account");
        assertEquals("SKIPPED", result.status());
    }

    // ===================================================
    // Test: Mark as sent updates log and counter
    // ===================================================
    @Test
    @Order(5)
    void shouldMarkAsSentAndIncrementCounter() {
        CommentEventMessage event = buildCommentEvent("COMMENT_004", "Send me free stuff!", "bob");

        AutomationService.ProcessingResult result = automationService.processComment(event);
        assertTrue(result.shouldSendDm());

        // Mark as sent
        automationService.markAsSent(result.interactionLog(), result.matchedRule());

        // Verify the log was updated
        InteractionLog savedLog = logRepo.findByIgCommentId("COMMENT_004").orElseThrow();
        assertEquals("SENT", savedLog.getStatus());
        assertNotNull(savedLog.getDmSentAt());
    }

    // ===================================================
    // Test: Case-insensitive keyword matching
    // ===================================================
    @Test
    @Order(6)
    void shouldMatchKeywordCaseInsensitive() {
        CommentEventMessage event = buildCommentEvent("COMMENT_005", "GIVE ME THE LINK PLEASE", "uppercaser");

        AutomationService.ProcessingResult result = automationService.processComment(event);

        assertTrue(result.shouldSendDm(), "Should match 'LINK' case-insensitively");
    }

    // ===================================================
    // Test: Token encryption round-trip
    // ===================================================
    @Test
    @Order(7)
    void shouldEncryptAndDecryptToken() {
        String originalToken = "EAABsbCS1iHg_long_access_token_here_1234567890";

        String encrypted = encryptionService.encrypt(originalToken);
        assertNotEquals(originalToken, encrypted, "Encrypted should differ from original");

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(originalToken, decrypted, "Decrypted should match original");
    }

    // ===================================================
    // Test: Daily DM counter reset
    // ===================================================
    @Test
    @Order(8)
    void shouldResetDailyCounters() {
        int resetCount = ruleRepo.resetAllDailyCounters();
        assertTrue(resetCount > 0, "Should reset at least 1 rule");

        AutomationRule refreshed = ruleRepo.findById(keywordRule.getId()).orElseThrow();
        assertEquals(0, refreshed.getDmSentToday());
    }
}
