package com.andy.fallboot.user.component;

import com.andy.fallboot.user.User;
import com.andy.fallboot.user.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(value = "users", key = "#cognitoId")
    public UserDTO findOrCreateUser(String cognitoId, String email, String name){
        return UserDTO.toUserDTO(userRepository.findByCognitoId(cognitoId).orElseGet(() -> userRepository.save(new User(cognitoId, email, name))));
    }
}
