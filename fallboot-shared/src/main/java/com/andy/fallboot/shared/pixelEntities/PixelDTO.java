package com.andy.fallboot.shared.pixelEntities;

import com.andy.fallboot.shared.userEntities.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public record PixelDTO(UUID roomUuid, int x, int y, String color, String lastUpdatedBy) {
    public static PixelDTO toPixelDTO(Pixel pixel){
        return new PixelDTO(pixel.getRoom().getId(), pixel.getX(), pixel.getY(), pixel.getColor(),
                Optional.ofNullable(pixel.getLastUpdatedBy()).map(User::getCognitoId).orElse(null));
    }

    public static PixelVerificationResponseDTO toVerificationResponseDTO(UUID roomID, Map<String, PixelDTO> pixelsMap) {
        final Map<String, String> pixelsResponseMap = pixelsMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().color()));
        return new PixelVerificationResponseDTO(roomID, pixelsResponseMap);
    }
}
