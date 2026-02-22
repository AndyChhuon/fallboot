package com.andy.fallboot.user;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true, nullable = false)
    private String cognitoId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    protected User() {}

    public User(String cognitoId, String email, String name) {
        this.email = email;
        this.cognitoId = cognitoId;
        this.name = name;
    }
}
