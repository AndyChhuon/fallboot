package com.andy.fallboot.room;

import java.util.UUID;

public record RoomDTO(UUID id, String roomName) {
    public static RoomDTO toRoomDTO(Room room) {
        return new RoomDTO(room.getId(), room.getRoomName());
    }
}
