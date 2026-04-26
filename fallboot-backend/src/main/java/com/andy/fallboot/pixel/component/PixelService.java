package com.andy.fallboot.pixel.component;

import com.andy.fallboot.shared.pixelEntities.Pixel;
import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import com.andy.fallboot.shared.pixelEntities.PixelVerificationResponseDTO;
import com.andy.fallboot.shared.pixelEntities.RoomPixelsResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PixelService {
    private final PixelRepository pixelRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PixelEventProducer pixelEventProducer;
    private final String cdnBaseUrl;

    public PixelService(PixelRepository pixelRepository,
                        RedisTemplate<String, Object> redisTemplate,
                        PixelEventProducer pixelEventProducer,
                        @Value("${fallboot.cdn.base-url}") String cdnBaseUrl) {
        this.pixelRepository = pixelRepository;
        this.redisTemplate = redisTemplate;
        this.pixelEventProducer = pixelEventProducer;
        this.cdnBaseUrl = cdnBaseUrl;
    }

    @Async("pixelPersistExecutor")
    public void cacheAndEmitPixelDTO(UUID roomId, int x, int y, String color, String cognitoId) {
        pixelEventProducer.sendPixelUpdate(new PixelDTO(roomId, x, y, color, cognitoId));
    }

    public RoomPixelsResponseDTO getRoomPixels(UUID roomId) {
        long seq = readSeq(roomId);
        return new RoomPixelsResponseDTO(roomId, snapshotUrl(roomId, seq), seq);
    }

    public PixelVerificationResponseDTO getRoomPixelsFromDB(UUID roomId) {
        Map<String, String> pixelColorsMap = pixelRepository.findByRoomId(roomId).stream()
                .collect(Collectors.toConcurrentMap(p -> p.getX() + ":" + p.getY(), Pixel::getColor));
        return new PixelVerificationResponseDTO(roomId, pixelColorsMap);
    }

    private long readSeq(UUID roomId) {
        Object raw = redisTemplate.opsForValue().get("room:" + roomId + ":snapshotSeq");
        if (raw == null) return 0L;
        if (raw instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String snapshotUrl(UUID roomId, long seq) {
        return cdnBaseUrl + "/rooms/" + roomId + ".png?v=" + seq;
    }
}
