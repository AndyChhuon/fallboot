package com.andy.fallboot.user;

import com.andy.fallboot.shared.userEntities.User;
import com.andy.fallboot.shared.userEntities.UserDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final Cache<String, UserDTO> localCache = Caffeine.newBuilder()
            .maximumSize(1_000_000)
            .build();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDTO findOrCreateUser(String cognitoId, String email, String name){
        return localCache.get(cognitoId, _ -> UserDTO.toUserDTO(
                userRepository.findById(cognitoId)
                        .orElseGet(() -> userRepository.save(new User(cognitoId, email, name)))
        ));
    }
}
