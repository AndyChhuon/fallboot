package com.andy.fallboot.kafka.pixel;

import com.andy.fallboot.shared.pixelEntities.Pixel;
import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PixelEventConsumer {
    private final PixelService pixelService;

    public PixelEventConsumer(PixelService pixelService){
        this.pixelService = pixelService;
    }

    @KafkaListener(
            topics = "${spring.kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "5"
    )
    public void listen(PixelDTO pixelDTO) {
        try {
            pixelService.saveToRepository(pixelDTO);
        } catch (Exception e) {

        }
    }

}
