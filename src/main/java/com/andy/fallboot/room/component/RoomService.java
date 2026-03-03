package com.andy.fallboot.room.component;

import com.andy.fallboot.room.Room;
import com.andy.fallboot.room.RoomDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {
    private RoomRepository roomRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void createRoom(String roomName){
        roomRepository.save(new Room(roomName));
    }

    public List<RoomDTO> getAllRooms() {
        return roomRepository.findAll().stream().map(RoomDTO::toRoomDTO).toList();
    }
}
