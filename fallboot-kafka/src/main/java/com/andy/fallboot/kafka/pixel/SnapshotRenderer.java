package com.andy.fallboot.kafka.pixel;

import com.andy.fallboot.shared.pixelEntities.Pixel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class SnapshotRenderer {
    private static final Logger log = LoggerFactory.getLogger(SnapshotRenderer.class);
    private static final int W = 1000;
    private static final int H = 1000;
    private static final int PIXEL_COUNT = W * H;

    private final ConcurrentHashMap<UUID, RoomCanvas> rooms = new ConcurrentHashMap<>();

    private final S3Client s3;
    private final StringRedisTemplate redisTemplate;
    private final PixelRepository pixelRepository;
    private final String bucket;
    private final String cdnBaseUrl;

    public SnapshotRenderer(S3Client s3,
                            StringRedisTemplate redisTemplate,
                            PixelRepository pixelRepository,
                            @Value("${aws.s3.bucket}") String bucket,
                            @Value("${fallboot.cdn.base-url}") String cdnBaseUrl) {
        this.s3 = s3;
        this.redisTemplate = redisTemplate;
        this.pixelRepository = pixelRepository;
        this.bucket = bucket;
        this.cdnBaseUrl = cdnBaseUrl;
    }

    public void applyPixel(UUID roomId, int x, int y, String hexColor) {
        if (x < 0 || x >= W || y < 0 || y >= H) return;
        RoomCanvas canvas = rooms.computeIfAbsent(roomId, this::hydrate);
        int argb = parseHex(hexColor);
        canvas.lock.lock();
        try {
            canvas.argb[y * W + x] = argb;
            canvas.dirty = true;
        } finally {
            canvas.lock.unlock();
        }
    }

    private RoomCanvas hydrate(UUID roomId) {
        RoomCanvas c = new RoomCanvas();
        try {
            for (Pixel p : pixelRepository.findByRoomId(roomId)) {
                int x = p.getX();
                int y = p.getY();
                if (x >= 0 && x < W && y >= 0 && y < H) {
                    c.argb[y * W + x] = parseHex(p.getColor());
                }
            }
            c.dirty = true;
        } catch (Exception e) {
            log.warn("Hydrate from DB failed for room {}: {}", roomId, e.getMessage());
        }
        return c;
    }

    @Scheduled(fixedRateString = "${fallboot.snapshot.flush-ms:250}")
    public void flush() {
        rooms.forEach((roomId, canvas) -> {
            int[] snapshot;
            canvas.lock.lock();
            try {
                if (!canvas.dirty) return;
                snapshot = canvas.argb.clone();
                canvas.dirty = false;
            } finally {
                canvas.lock.unlock();
            }
            try {
                byte[] png = encodePng(snapshot);
                s3.putObject(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key("rooms/" + roomId + ".png")
                                .contentType("image/png")
                                .cacheControl("public, max-age=31536000, immutable")
                                .build(),
                        RequestBody.fromBytes(png));
                Long seq = redisTemplate.opsForValue().increment("room:" + roomId + ":snapshotSeq");
                long seqVal = seq == null ? 0L : seq;
                String url = cdnBaseUrl + "/rooms/" + roomId + ".png?v=" + seqVal;
                String payload = "{\"seq\":" + seqVal + ",\"url\":\"" + url + "\"}";
                redisTemplate.convertAndSend("pixel-snapshot:" + roomId, payload);
            } catch (Exception e) {
                log.error("Snapshot render failed for room {}: {}", roomId, e.getMessage());
                canvas.lock.lock();
                try {
                    canvas.dirty = true;
                } finally {
                    canvas.lock.unlock();
                }
            }
        });
    }

    private byte[] encodePng(int[] argb) throws IOException {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, W, H, argb, 0, W);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private int parseHex(String color) {
        if (color == null || color.isEmpty()) return 0;
        String hex = color.startsWith("#") ? color.substring(1) : color;
        try {
            int rgb = Integer.parseInt(hex, 16);
            return 0xFF000000 | (rgb & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static class RoomCanvas {
        final int[] argb = new int[PIXEL_COUNT];
        final ReentrantLock lock = new ReentrantLock();
        volatile boolean dirty;
    }
}
