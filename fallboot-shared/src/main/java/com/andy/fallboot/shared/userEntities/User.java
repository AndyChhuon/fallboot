package com.andy.fallboot.shared.userEntities;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    private String cognitoId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    protected User() {}

    public User(String cognitoId, String email, String name) {
        this.cognitoId = cognitoId;
        this.email = email;
        this.name = name;
    }

    public String getCognitoId() {
        return cognitoId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
}
