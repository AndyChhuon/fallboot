package com.andy.fallboot.room;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class Room {
    @Id
    @GeneratedValue
    private UUID id;

    private String roomName;

    protected Room() {}

    public Room(String roomName) {
        this.roomName = roomName;
    }
}
