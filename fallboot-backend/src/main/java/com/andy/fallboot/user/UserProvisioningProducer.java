package com.andy.fallboot.user;

import com.andy.fallboot.shared.userEntities.UserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserProvisioningProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String kafkaTopicName;

    public UserProvisioningProducer(@Value("${spring.kafka.topic.name}") String kafkaTopicName,
                                    KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopicName = kafkaTopicName;
    }

    public void sendUserProvisioning(UserDTO userDTO) {
        kafkaTemplate.send(kafkaTopicName, userDTO.cognitoId(), userDTO);
    }
}
