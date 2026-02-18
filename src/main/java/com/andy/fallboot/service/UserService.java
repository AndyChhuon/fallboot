package com.andy.fallboot.service;

import com.andy.fallboot.entity.User;
import com.andy.fallboot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void findOrCreateUser(String cognitoId, String email, String name){
        Optional<User> user = userRepository.findByCognitoId(cognitoId);
        user.orElseGet(() -> userRepository.save(new User(cognitoId, email, name)));
    }
}
