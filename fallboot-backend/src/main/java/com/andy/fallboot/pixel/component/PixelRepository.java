package com.andy.fallboot.pixel.component;

import com.andy.fallboot.shared.pixelEntities.Pixel;
import com.andy.fallboot.shared.pixelEntities.PixelId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PixelRepository extends JpaRepository<Pixel, PixelId> {
    List<Pixel> findByRoomId(UUID roomId);
}
