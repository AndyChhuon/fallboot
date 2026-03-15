package com.andy.fallboot.shared.userEntities;

public record UserDTO(Long id, String cognitoId, String email, String name) {
    public static UserDTO toUserDTO(User user) {
        return new UserDTO(user.getId(), user.getCognitoId(), user.getEmail(), user.getName());
    }
}
