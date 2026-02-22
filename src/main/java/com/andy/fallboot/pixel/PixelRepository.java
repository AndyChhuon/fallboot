package com.andy.fallboot.pixel;

import com.andy.fallboot.room.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PixelRepository extends JpaRepository<Pixel, PixelId> {
    @Modifying
    @Query(value = """
      INSERT INTO pixels (room_id, x, y, color, last_updated_by)
      VALUES (:roomId, :x, :y, :color, :userId)
      ON CONFLICT (room_id, x, y) DO UPDATE SET color = :color, last_updated_by = :userId
      """, nativeQuery = true)
    void upsertPixel(@Param("roomId") UUID roomId,
                     @Param("x") int x,
                     @Param("y") int y,
                     @Param("color") String color,
                     @Param("userId") Long userId);

    List<Pixel> findByRoomId(UUID roomId);
}
