package com.andy.fallboot.pixel.component;

import com.andy.fallboot.pixel.PixelDTO;
import com.andy.fallboot.pixel.PixelMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
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
            PixelMessage message, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        pixelService.setPixelColor(roomId, message.getX(), message.getY(), message.getColor(), userId);
        return message;
    }

    @GetMapping("/api/pixels/room/{roomId}")
    @ResponseBody
    public List<PixelDTO> getAllPixelsInRoom(@PathVariable UUID roomId){
        return pixelService.getRoomPixels(roomId);
    }
}
