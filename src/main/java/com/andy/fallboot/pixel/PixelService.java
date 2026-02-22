package com.andy.fallboot.pixel;

import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PixelService {
    private final PixelRepository pixelRepository;

    public PixelService(PixelRepository pixelRepository){
        this.pixelRepository = pixelRepository;
    }

    public void setPixelColor(UUID roomId, int x, int y, String color, Long userId){
        pixelRepository.upsertPixel(roomId, x, y, color, userId);
    }

    public List<Pixel> getRoomPixels(UUID roomId) {
        return pixelRepository.findByRoomId(roomId);
    }
}
