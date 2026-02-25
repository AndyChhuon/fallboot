package com.andy.fallboot.pixel.component;

import com.andy.fallboot.pixel.PixelMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class PixelMessageController {
    private final PixelService pixelService;

    public PixelMessageController(PixelService pixelService) {
        this.pixelService = pixelService;
    }

    @MessageMapping("/update-pixel/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public PixelMessage updatePixelColor(
            @DestinationVariable("roomId") UUID roomId,
            PixelMessage message) {
        pixelService.setPixelColor(roomId, message.getX(), message.getY(), message.getColor(), null);
        return message;
    }
}
