package com.andy.fallboot.kafka.pixel;

import com.andy.fallboot.shared.userEntities.User;
import com.andy.fallboot.shared.userEntities.UserDTO;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void provisionUser(UserDTO userDTO) {
        if (!userRepository.existsById(userDTO.cognitoId())) {
            userRepository.save(new User(userDTO.cognitoId(), userDTO.email(), userDTO.name()));
        }
    }
}
