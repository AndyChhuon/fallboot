package com.andy.fallboot.pixel.component;

import com.andy.fallboot.user.UserProvisioningProducer;
import com.andy.fallboot.shared.userEntities.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

@Component
public class PixelWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(PixelWebSocketHandler.class);
    private final JwtDecoder jwtDecoder;
    private final PixelService pixelService;
    private final BroadcastSessionManager broadcastSessionManager;
    private final UserProvisioningProducer userProvisioningProducer;
    private final ObjectMapper objectMapper;

    public PixelWebSocketHandler(JwtDecoder jwtDecoder, PixelService pixelService,
                                  BroadcastSessionManager broadcastSessionManager,
                                  UserProvisioningProducer userProvisioningProducer,
                                  ObjectMapper objectMapper) {
        this.jwtDecoder = jwtDecoder;
        this.pixelService = pixelService;
        this.broadcastSessionManager = broadcastSessionManager;
        this.userProvisioningProducer = userProvisioningProducer;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpJwtDecoder() {
        try {
            jwtDecoder.decode("dummy");
        } catch (Exception e) {
            log.info("JWT decoder warmed up (JWKS fetched)");
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcastSessionManager.register(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.path("type").asText();

        switch (type) {
            case "auth" -> handleAuth(session, json);
            case "subscribe" -> handleSubscribe(session, json);
            case "pixel" -> handlePixelUpdate(session, json);
        }
    }

    private void handleAuth(WebSocketSession session, JsonNode json) throws Exception {
        String token = json.path("token").asText();
        long start = System.currentTimeMillis();
        try {
            Jwt jwt = jwtDecoder.decode(token);
            long decodeTime = System.currentTimeMillis() - start;
            if (decodeTime > 1000) {
                log.warn("Slow JWT decode: {}ms for session {}", decodeTime, session.getId());
            }

            String cognitoId = jwt.getClaimAsString("sub");
            String email = jwt.getClaimAsString("email");
            String name = jwt.getClaimAsString("given_name");

            session.getAttributes().put("cognitoId", cognitoId);
            userProvisioningProducer.sendUserProvisioning(new UserDTO(cognitoId, email, name));

            session.sendMessage(new TextMessage("{\"type\":\"authenticated\"}"));
        } catch (Exception e) {
            log.error("Auth failed for session {}: {}", session.getId(), e.getMessage());
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"Invalid token\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    private void handleSubscribe(WebSocketSession session, JsonNode json) {
        String roomId = json.path("roomId").asText();
        session.getAttributes().put("roomId", roomId);
        broadcastSessionManager.markReady(session.getId());
    }

    private void handlePixelUpdate(WebSocketSession session, JsonNode json) {
        String cognitoId = (String) session.getAttributes().get("cognitoId");
        if (cognitoId == null) return;

        UUID roomId = UUID.fromString(json.path("roomId").asText());
        int x = json.path("x").asInt();
        int y = json.path("y").asInt();
        String color = json.path("color").asText();

        pixelService.cacheAndEmitPixelDTO(roomId, x, y, color, cognitoId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcastSessionManager.unregister(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        broadcastSessionManager.unregister(session.getId());
    }
}
