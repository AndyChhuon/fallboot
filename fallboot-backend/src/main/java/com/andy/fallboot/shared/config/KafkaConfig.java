package com.andy.fallboot.shared.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    private final String kafkaTopicName;

    public KafkaConfig(@Value("${spring.kafka.topic.name}") String kafkaTopicName) {
        this.kafkaTopicName = kafkaTopicName;
    }
    @Bean
    public NewTopic pixelUpdatesTopic() {
        return TopicBuilder.name(kafkaTopicName)
                .partitions(50)
                .build();
    }
}
