package com.andy.fallboot.pixel.component;

import com.andy.fallboot.pixel.PixelMessage;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PixelBroadcastSubscriber implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(PixelBroadcastSubscriber.class);
    private final BroadcastSessionManager sessionManager;
    private final PixelService pixelService;
    private final RedisSerializer<Object> serializer;
    private final ObjectMapper objectMapper;

    public PixelBroadcastSubscriber(BroadcastSessionManager sessionManager, PixelService pixelService, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.pixelService = pixelService;
        this.serializer = RedisSerializer.json();
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String roomIdStr = channel.replace("pixel-broadcast:", "");

            Object batch = serializer.deserialize(message.getBody());
            String jsonPayload = new String(serializer.serialize(batch));

            UUID roomId = UUID.fromString(roomIdStr);
            List<PixelMessage> pixels = objectMapper.convertValue(batch, new TypeReference<>() {});
            pixelService.updateLocalCache(roomId, pixels);

            String stompFrame = "MESSAGE\n" +
                    "destination:/topic/room." + roomIdStr + "\n" +
                    "content-type:application/json\n" +
                    "content-length:" + jsonPayload.length() + "\n" +
                    "\n" +
                    jsonPayload + "\0";

            sessionManager.broadcast(stompFrame);
        } catch (Exception e) {
            log.error("Error processing Redis broadcast: {}", e.getMessage());
        }
    }
}
