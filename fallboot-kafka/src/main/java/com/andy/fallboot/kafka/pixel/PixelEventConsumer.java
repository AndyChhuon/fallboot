package com.andy.fallboot.kafka.pixel;

import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import com.andy.fallboot.shared.userEntities.UserDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PixelEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PixelEventConsumer.class);
    private final PixelService pixelService;
    private final UserService userService;
    private final SnapshotRenderer snapshotRenderer;

    public PixelEventConsumer(PixelService pixelService, UserService userService, SnapshotRenderer snapshotRenderer){
        this.pixelService = pixelService;
        this.userService = userService;
        this.snapshotRenderer = snapshotRenderer;
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "5"
    )
    public void listen(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        try {
            switch (event) {
                case UserDTO userDTO -> userService.provisionUser(userDTO);
                case PixelDTO pixelDTO -> {
                    pixelService.saveToRepository(pixelDTO);
                    snapshotRenderer.applyPixel(pixelDTO.roomUuid(), pixelDTO.x(), pixelDTO.y(), pixelDTO.color());
                }
                case null -> log.warn("Null event payload");
                default -> log.warn("Unknown event type: {}", event.getClass().getName());
            }
        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage());
        }
    }
}
