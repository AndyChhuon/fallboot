package com.andy.fallboot.pixel.component;

import com.andy.fallboot.pixel.PixelMessage;
import com.andy.fallboot.shared.pixelEntities.Pixel;
import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import com.andy.fallboot.shared.pixelEntities.RoomPixelsResponseDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class PixelService {
    private static final Logger log = LoggerFactory.getLogger(PixelService.class);
    private final PixelRepository pixelRepository;
    private final HashOperations<String, String, PixelDTO> hashOps;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<UUID, ConcurrentMap<String, String>> localCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofMinutes(3))
            .build();
    private final PixelEventProducer pixelEventProducer;

    public PixelService(PixelRepository pixelRepository, RedisTemplate<String, Object> redisTemplate, PixelEventProducer pixelEventProducer){
        this.pixelRepository = pixelRepository;
        this.hashOps = redisTemplate.opsForHash();
        this.redisTemplate = redisTemplate;
        this.pixelEventProducer = pixelEventProducer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmCache() {
        try {
            pixelRepository.findAll().stream()
                    .collect(Collectors.groupingBy(p -> p.getRoom().getId()))
                    .forEach((roomId, pixels) -> {
                        ConcurrentMap<String, String> pixelMap = pixels.stream()
                                .collect(Collectors.toConcurrentMap(
                                        p -> p.getX() + ":" + p.getY(),
                                        Pixel::getColor
                                ));
                        localCache.put(roomId, pixelMap);
                    });
            localCache.asMap().computeIfAbsent(
                    UUID.fromString("c55c81a0-806c-4108-b393-500d88851d88"),
                    _ -> new ConcurrentHashMap<>()
            );
        } catch (Exception e) {
            log.warn("Cache warm-up failed (will load lazily): {}", e.getMessage());
        }
    }

    @Async("pixelPersistExecutor")
    public void cacheAndEmitPixelDTO(UUID roomId, int x, int y, String color, String cognitoId){
        final PixelDTO pixelDTO = new PixelDTO(roomId,x,y,color,cognitoId);
        pixelEventProducer.sendPixelUpdate(pixelDTO);
        localCache.asMap().computeIfPresent(roomId, (key, cachedMap) -> {
            cachedMap.put(x + ":" + y, color);
            return cachedMap;
        });
    }

    public RoomPixelsResponseDTO getRoomPixels(UUID roomId) {
        ConcurrentMap<String, String> cached = localCache.getIfPresent(roomId);
        if (cached != null) {
            return new RoomPixelsResponseDTO(roomId, cached);
        }

        final String roomKey = "room:" + roomId + ":pixels";
        Map<String, String> pixelColorsMap = localCache.get(roomId, _ -> {
            Map<String, PixelDTO> cachedPixels = hashOps.entries(roomKey);
            if (!cachedPixels.isEmpty()) {
                return cachedPixels.entrySet().stream().collect(Collectors.toConcurrentMap(Map.Entry::getKey, entry -> entry.getValue().color()));
            }
            Map<String, PixelDTO> pixelsFromDB = pixelRepository.findByRoomId(roomId).stream().map(PixelDTO::toPixelDTO).collect(Collectors.toMap(pixel -> pixel.x() + ":" + pixel.y(), pixel -> pixel));
            hashOps.putAll(roomKey, pixelsFromDB);
            redisTemplate.expire(roomKey, Duration.ofMinutes(5));
            return pixelsFromDB.entrySet().stream().collect(Collectors.toConcurrentMap(Map.Entry::getKey, entry -> entry.getValue().color()));
        });

        return new RoomPixelsResponseDTO(roomId, pixelColorsMap);
    }

    public void updateLocalCache(UUID roomId, List<PixelMessage> batch) {
        localCache.asMap().computeIfPresent(roomId, (key, cachedMap) -> {
            for (PixelMessage pixel : batch) {
                cachedMap.put(pixel.getX() + ":" + pixel.getY(), pixel.getColor());
            }
            return cachedMap;
        });
    }

    public RoomPixelsResponseDTO getRoomPixelsFromDB(UUID roomId) {
        Map<String, String> pixelColorsMap = pixelRepository.findByRoomId(roomId).stream().collect(Collectors.toConcurrentMap(pixel -> pixel.getX() + ":" + pixel.getY(), Pixel::getColor));
        return new RoomPixelsResponseDTO(roomId, pixelColorsMap);
    }
}
