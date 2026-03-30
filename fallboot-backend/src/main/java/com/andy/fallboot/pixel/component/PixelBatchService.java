package com.andy.fallboot.pixel.component;

import com.andy.fallboot.pixel.PixelMessage;
import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class PixelBatchService {
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<PixelMessage>> buffers = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, PixelDTO> hashOps;

    public PixelBatchService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    public void addToBuffer(UUID roomId, PixelMessage message){
        buffers.computeIfAbsent(roomId, _ -> new ConcurrentLinkedDeque<>()).add(message);
    }

    @Scheduled(fixedRate = 100)
    public void flush() {
        buffers.forEach((roomId, queue) -> {
            List<PixelMessage> batch = new ArrayList<>();
            PixelMessage msg;
            while ((msg = queue.poll()) != null) {
                batch.add(msg);
            }
            if (!batch.isEmpty()) {
                String roomKey = "room:" + roomId + ":pixels";
                Map<String, PixelDTO> redisUpdates = new HashMap<>();
                for (PixelMessage pixel : batch) {
                    redisUpdates.put(pixel.getX() + ":" + pixel.getY(),
                            new PixelDTO(roomId, pixel.getX(), pixel.getY(), pixel.getColor(), pixel.getLastUpdatedBy()));
                }
                hashOps.putAll(roomKey, redisUpdates);

                // Publish to Redis Pub/Sub for cross-instance broadcasting
                redisTemplate.convertAndSend("pixel-broadcast:" + roomId, batch);
            }
        });
    }
}
