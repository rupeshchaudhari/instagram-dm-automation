package com.igdm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events (SSE) notification manager.
 * Broadcasts real-time interaction log events to connected frontend clients.
 */
@Service
public class SseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(60000L * 30); // 30 min timeout

        emitters.add(emitter);
        log.info("Client subscribed to real-time SSE stream. Active emitters: {}", emitters.size());

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(Map.of("message", "Connected to InstaAuto DM Real-time Stream")));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcastInteractionLog(String logId, String username, String commentText, String status, String ruleName) {
        if (emitters.isEmpty()) return;

        Map<String, Object> payload = Map.of(
                "id", logId,
                "username", username,
                "commentText", commentText,
                "status", status,
                "ruleName", ruleName,
                "timestamp", System.currentTimeMillis()
        );

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("INTERACTION_EVENT")
                        .data(payload));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}
