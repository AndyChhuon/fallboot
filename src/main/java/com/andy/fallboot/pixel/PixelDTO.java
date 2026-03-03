package com.andy.fallboot.pixel;

import com.andy.fallboot.user.User;

import java.util.Optional;
import java.util.UUID;

public record PixelDTO(UUID roomUuid, int x, int y, String color, Long lastUpdatedBy) {
    public static PixelDTO toPixelDTO(Pixel pixel){
        return new PixelDTO(pixel.getRoom().getId(), pixel.getX(), pixel.getY(), pixel.getColor(), Optional.ofNullable(pixel.getLastUpdatedBy()).map(User::getId).orElse(null));
    }
}
