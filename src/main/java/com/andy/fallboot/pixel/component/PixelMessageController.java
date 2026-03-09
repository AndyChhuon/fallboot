package com.andy.fallboot.pixel.component;

import com.andy.fallboot.pixel.PixelDTO;
import com.andy.fallboot.pixel.PixelMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Controller
public class PixelMessageController {
    private static final Logger log = LoggerFactory.getLogger(PixelMessageController.class);
    private final PixelService pixelService;

    public PixelMessageController(PixelService pixelService) {
        this.pixelService = pixelService;
    }

    @MessageMapping("/update-pixel/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public PixelMessage updatePixelColor(
            @DestinationVariable("roomId") UUID roomId,
            PixelMessage message, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        log.info("Pixel update: user={} room={} x={} y={} color={}", userId, roomId, message.getX(), message.getY(), message.getColor());

        pixelService.setPixelColor(roomId, message.getX(), message.getY(), message.getColor(), userId);
        message.setLastUpdatedBy(userId);
        return message;
    }

    @GetMapping("/api/pixels/room/{roomId}")
    @ResponseBody
    public Map<String, PixelDTO> getAllPixelsInRoom(@PathVariable UUID roomId){
        return pixelService.getRoomPixels(roomId);
    }
}
