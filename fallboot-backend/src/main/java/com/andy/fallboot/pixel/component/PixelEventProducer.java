package com.andy.fallboot.pixel.component;

import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PixelEventProducer {
    private final KafkaTemplate<String, PixelDTO> kafkaTemplate;
    private final String kafkaTopicName;

    public PixelEventProducer(@Value("${spring.kafka.topic.name}") String kafkaTopicName, KafkaTemplate<String, PixelDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopicName = kafkaTopicName;
    }

    public void sendPixelUpdate(PixelDTO message) {
        kafkaTemplate.send(kafkaTopicName, message.roomUuid() + ":" + message.x() + ":" + message.y(), message);
    }
}
