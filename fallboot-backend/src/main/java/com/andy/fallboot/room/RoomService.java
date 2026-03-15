package com.andy.fallboot.room;

import com.andy.fallboot.shared.roomEntities.Room;
import com.andy.fallboot.shared.roomEntities.RoomDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class RoomService {
    private static final String ROOMS_KEY = "all_rooms";
    private final RoomRepository roomRepository;
    private final Cache<String, List<RoomDTO>> localCache = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Autowired
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public void createRoom(String roomName){
        roomRepository.save(new Room(roomName));
        localCache.invalidate(ROOMS_KEY);
    }

    public List<RoomDTO> getAllRooms() {
        return localCache.get(ROOMS_KEY, _ ->
                roomRepository.findAll().stream().map(RoomDTO::toRoomDTO).toList());
    }
}
