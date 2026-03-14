package com.andy.fallboot.pixel.component;

import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PixelService {
    private final PixelRepository pixelRepository;
    private final HashOperations<String, String, PixelDTO> hashOps;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<UUID, Map<String, PixelDTO>> localCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofSeconds(10))
            .build();

    public PixelService(PixelRepository pixelRepository, RedisTemplate<String, Object> redisTemplate){
        this.pixelRepository = pixelRepository;
        this.hashOps = redisTemplate.opsForHash();
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    @Async("pixelPersistExecutor")
    public void setPixelColor(UUID roomId, int x, int y, String color, Long userId){
        pixelRepository.upsertPixel(roomId, x, y, color, userId);
        hashOps.put("room:"+roomId+":pixels", x+":"+y, new PixelDTO(roomId,x,y,color,userId));
        localCache.invalidate(roomId);
    }

    public Map<String, PixelDTO> getRoomPixels(UUID roomId) {
        final String roomKey = "room:"+roomId+":pixels";

        return localCache.get(roomId, _ -> {
            Map<String, PixelDTO> cachedPixels = hashOps.entries(roomKey);
            if (!cachedPixels.isEmpty()) {
                return cachedPixels;
            }
            Map<String, PixelDTO> pixelsFromDB = pixelRepository.findByRoomId(roomId).stream().map(PixelDTO::toPixelDTO).collect(Collectors.toMap(pixel -> pixel.x()+":"+pixel.y(), pixel -> pixel));
            hashOps.putAll(roomKey, pixelsFromDB);

            redisTemplate.expire(roomKey, Duration.ofMinutes(5));
            return pixelsFromDB;
        });
    }
}
