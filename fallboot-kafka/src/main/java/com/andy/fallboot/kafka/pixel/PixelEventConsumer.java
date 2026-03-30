package com.andy.fallboot.kafka.pixel;

import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import com.andy.fallboot.shared.userEntities.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PixelEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PixelEventConsumer.class);
    private final PixelService pixelService;
    private final UserService userService;

    public PixelEventConsumer(PixelService pixelService, UserService userService){
        this.pixelService = pixelService;
        this.userService = userService;
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "5"
    )
    public void listen(Object event) {
        try {
            switch (event) {
                case UserDTO userDTO -> userService.provisionUser(userDTO);
                case PixelDTO pixelDTO -> pixelService.saveToRepository(pixelDTO);
                default -> log.warn("Unknown event type: {}", event.getClass().getName());
            }
        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage());
        }
    }
}
