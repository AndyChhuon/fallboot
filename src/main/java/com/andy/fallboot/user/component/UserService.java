package com.andy.fallboot.user.component;

import com.andy.fallboot.user.User;
import com.andy.fallboot.user.UserDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final Cache<String, UserDTO> localCache = Caffeine.newBuilder()
            .maximumSize(5_000)
            .build();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDTO findOrCreateUser(String cognitoId, String email, String name){
        return localCache.get(cognitoId, _ -> UserDTO.toUserDTO(userRepository.findByCognitoId(cognitoId).orElseGet(() -> userRepository.save(new User(cognitoId, email, name)))));
    }
}
