package com.andy.fallboot.kafka.pixel;

import com.andy.fallboot.shared.pixelEntities.Pixel;
import com.andy.fallboot.shared.pixelEntities.PixelId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PixelRepository extends JpaRepository<Pixel, PixelId> {
    @Modifying
    @Query(value = """
      INSERT INTO pixel (room_id, x, y, color, last_updated_by_id)
      VALUES (:roomId, :x, :y, :color, :userId)
      ON CONFLICT (room_id, x, y) DO UPDATE SET color = :color, last_updated_by_id = :userId
      """, nativeQuery = true)
    void upsertPixel(@Param("roomId") UUID roomId,
                     @Param("x") int x,
                     @Param("y") int y,
                     @Param("color") String color,
                     @Param("userId") Long userId);
}
