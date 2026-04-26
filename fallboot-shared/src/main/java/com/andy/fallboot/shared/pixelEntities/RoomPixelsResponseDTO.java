package com.andy.fallboot.shared.pixelEntities;

import java.util.UUID;

public record RoomPixelsResponseDTO(UUID roomUID, String snapshotUrl, long seq) {
}
