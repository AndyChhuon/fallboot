package com.andy.fallboot.shared.pixelEntities;

import java.util.Map;
import java.util.UUID;

public record PixelVerificationResponseDTO(UUID roomUID, Map<String, String> pixelsMap) {
}
