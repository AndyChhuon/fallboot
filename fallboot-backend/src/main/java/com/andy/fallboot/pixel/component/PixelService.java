package com.andy.fallboot.pixel.component;

import com.andy.fallboot.shared.pixelEntities.Pixel;
import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import com.andy.fallboot.shared.pixelEntities.RoomPixelsResponseDTO;
import com.andy.fallboot.shared.roomEntities.Room;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class PixelService {
    private final PixelRepository pixelRepository;
    private final HashOperations<String, String, PixelDTO> hashOps;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<UUID, ConcurrentMap<String, String>> localCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofSeconds(10))
            .build();
    private final PixelEventProducer pixelEventProducer;

    public PixelService(PixelRepository pixelRepository, RedisTemplate<String, Object> redisTemplate, PixelEventProducer pixelEventProducer){
        this.pixelRepository = pixelRepository;
        this.hashOps = redisTemplate.opsForHash();
        this.redisTemplate = redisTemplate;
        this.pixelEventProducer = pixelEventProducer;
    }

    @Async("pixelPersistExecutor")
    public void cacheAndEmitPixelDTO(UUID roomId, int x, int y, String color, Long userId){
        final PixelDTO pixelDTO = new PixelDTO(roomId,x,y,color,userId);
        pixelEventProducer.sendPixelUpdate(pixelDTO);
        localCache.asMap().computeIfPresent(roomId, (key, cachedMap) -> {
            cachedMap.put(x + ":" + y, color);
            return cachedMap;
        });
    }

public RoomPixelsResponseDTO getRoomPixels(UUID roomId) {
        final String roomKey = "room:"+roomId+":pixels";

        Map<String, String> pixelColorsMap = localCache.get(roomId, _ -> {
            Map<String, PixelDTO> cachedPixels = hashOps.entries(roomKey);
            if (!cachedPixels.isEmpty()) {
                return cachedPixels.entrySet().stream().collect(Collectors.toConcurrentMap(Map.Entry::getKey, entry -> entry.getValue().color()));
            }
            Map<String, PixelDTO> pixelsFromDB = pixelRepository.findByRoomId(roomId).stream().map(PixelDTO::toPixelDTO).collect(Collectors.toMap(pixel -> pixel.x()+":"+pixel.y(), pixel -> pixel));
            hashOps.putAll(roomKey, pixelsFromDB);

            redisTemplate.expire(roomKey, Duration.ofMinutes(5));
            return pixelsFromDB.entrySet().stream().collect(Collectors.toConcurrentMap(Map.Entry::getKey, entry -> entry.getValue().color()));
        });

        return new RoomPixelsResponseDTO(roomId, pixelColorsMap);
    }

    public RoomPixelsResponseDTO getRoomPixelsFromDB(UUID roomId) {
        Map<String, String> pixelColorsMap = pixelRepository.findByRoomId(roomId).stream().collect(Collectors.toConcurrentMap(pixel -> pixel.getX() + ":" + pixel.getY(), Pixel::getColor));

        return new RoomPixelsResponseDTO(roomId, pixelColorsMap);
    }
}
