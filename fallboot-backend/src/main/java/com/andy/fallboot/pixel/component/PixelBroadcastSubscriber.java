package com.andy.fallboot.pixel.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PixelBroadcastSubscriber implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(PixelBroadcastSubscriber.class);
    private static final String CHANNEL_PREFIX = "pixel-snapshot:";
    private final BroadcastSessionManager sessionManager;

    public PixelBroadcastSubscriber(BroadcastSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            if (!channel.startsWith(CHANNEL_PREFIX)) return;
            String roomId = channel.substring(CHANNEL_PREFIX.length());

            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            String payload = "{\"type\":\"snapshot\"," + stripBraces(body) + "}";

            sessionManager.broadcast(roomId, payload);
        } catch (Exception e) {
            log.error("Error processing snapshot notification: {}", e.getMessage());
        }
    }

    private String stripBraces(String json) {
        String trimmed = json.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
