package com.andy.fallboot.pixel.component;

import com.andy.fallboot.shared.pixelEntities.PixelVerificationResponseDTO;
import com.andy.fallboot.shared.pixelEntities.RoomPixelsResponseDTO;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class PixelMessageController {
    private final PixelService pixelService;

    public PixelMessageController(PixelService pixelService) {
        this.pixelService = pixelService;
    }

    @GetMapping("/api/pixels/room/{roomId}")
    public RoomPixelsResponseDTO getAllPixelsInRoom(@PathVariable UUID roomId) {
        return pixelService.getRoomPixels(roomId);
    }

    @GetMapping("/api/pixels/room/test-verification/{roomId}")
    public PixelVerificationResponseDTO getAllPixelsInRoomFromDB(@PathVariable UUID roomId) {
        return pixelService.getRoomPixelsFromDB(roomId);
    }
}
