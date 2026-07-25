package com.igdm.service;

import com.igdm.dto.CommentEventMessage;
import com.igdm.dto.CommentEventMessage.CommentPayload;
import com.igdm.dto.CommentEventMessage.Commenter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutomationServiceUnitTest {

    private final AutomationService automationService = new AutomationService(null, null, null, null);

    @Test
    @DisplayName("Keyword Match Test: Comment containing 'link' should match rule")
    void testKeywordMatching() {
        List<String> keywords = List.of("link", "discount", "sale");

        assertTrue(automationService.matchesKeywords(keywords, "Send me the link please!"));
        assertTrue(automationService.matchesKeywords(keywords, "Is there a DISCOUNT available?"));
        assertFalse(automationService.matchesKeywords(keywords, "Great photo!"));
    }

    @Test
    @DisplayName("Template Rendering Test: {{username}} placeholder substitution")
    void testTemplateRendering() {
        CommentPayload payload = new CommentPayload();
        Commenter commenter = new Commenter();
        commenter.setUsername("sarah_growth");
        payload.setCommenter(commenter);
        payload.setCommentText("link please");

        String template = "Hey {{username}}! Here is your requested link: https://example.com";
        String rendered = automationService.renderTemplate(template, payload);

        assertEquals("Hey sarah_growth! Here is your requested link: https://example.com", rendered);
    }
}
