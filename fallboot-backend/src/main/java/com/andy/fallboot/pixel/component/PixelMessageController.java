package com.andy.fallboot.pixel.component;

import com.andy.fallboot.pixel.PixelMessage;
import com.andy.fallboot.shared.pixelEntities.RoomPixelsResponseDTO;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.UUID;

@Controller
public class PixelMessageController {
    private static final Logger log = LoggerFactory.getLogger(PixelMessageController.class);
    private final PixelService pixelService;
    private final PixelBatchService pixelBatchService;

    public PixelMessageController(PixelService pixelService, PixelBatchService pixelBatchService) {
        this.pixelService = pixelService;
        this.pixelBatchService = pixelBatchService;
    }

    @MessageMapping("/update-pixel/{roomId}")
    public void updatePixelColor(
            @DestinationVariable("roomId") UUID roomId,
            PixelMessage message, Principal principal) {
        String cognitoId = principal.getName();

        pixelService.cacheAndEmitPixelDTO(roomId, message.getX(), message.getY(), message.getColor(), cognitoId);
        message.setLastUpdatedBy(cognitoId);
        pixelBatchService.addToBuffer(roomId, message);
    }

    @GetMapping("/api/pixels/room/{roomId}")
    @ResponseBody
    public RoomPixelsResponseDTO getAllPixelsInRoom(@PathVariable UUID roomId){
        return pixelService.getRoomPixels(roomId);
    }

    @GetMapping("/api/pixels/room/test-verification/{roomId}")
    @ResponseBody
    public RoomPixelsResponseDTO getAllPixelsInRoomFromDB(@PathVariable UUID roomId){
        return pixelService.getRoomPixelsFromDB(roomId);
    }
}
