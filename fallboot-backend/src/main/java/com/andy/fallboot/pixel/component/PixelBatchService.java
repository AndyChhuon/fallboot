package com.andy.fallboot.pixel.component;

import com.andy.fallboot.pixel.PixelMessage;
import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
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
public class PixelBatchService implements ApplicationListener<BrokerAvailabilityEvent> {
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<PixelMessage>> buffers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final HashOperations<String, String, PixelDTO> hashOps;
    private volatile boolean brokerAvailable;

    public PixelBatchService(SimpMessagingTemplate messagingTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    public void addToBuffer(UUID roomId, PixelMessage message){
        buffers.computeIfAbsent(roomId, _ -> new ConcurrentLinkedDeque<>()).add(message);
    }

    @Override
    public void onApplicationEvent(BrokerAvailabilityEvent event) {
        this.brokerAvailable = event.isBrokerAvailable();
    }


    @Scheduled(fixedRate = 50)
    public void flush() {
        if (brokerAvailable) {
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

                    messagingTemplate.convertAndSend("/topic/room." + roomId, batch);
                }
            });
        }
    }
}
