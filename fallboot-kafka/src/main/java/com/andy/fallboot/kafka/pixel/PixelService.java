package com.andy.fallboot.kafka.pixel;

import com.andy.fallboot.shared.pixelEntities.PixelDTO;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PixelService {
    private final PixelRepository pixelRepository;
    private static final Logger log = LoggerFactory.getLogger(PixelService.class);

    public PixelService(PixelRepository pixelRepository){
        this.pixelRepository = pixelRepository;
    }

    @Transactional
    public void saveToRepository(PixelDTO pixelDTO){
        log.info("Upserting {} {} {}", pixelDTO.color(), pixelDTO.x(), pixelDTO.y());
        pixelRepository.upsertPixel(pixelDTO.roomUuid(), pixelDTO.x(), pixelDTO.y(), pixelDTO.color(), pixelDTO.lastUpdatedBy());
    }
}
