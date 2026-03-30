package com.andy.fallboot.shared.userEntities;

public record UserDTO(String cognitoId, String email, String name) {
    public static UserDTO toUserDTO(User user) {
        return new UserDTO(user.getCognitoId(), user.getEmail(), user.getName());
    }
}
