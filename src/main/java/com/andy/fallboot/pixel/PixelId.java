package com.andy.fallboot.pixel;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PixelId implements Serializable {
    private UUID room;
    private int x;
    private int y;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PixelId pixelId = (PixelId) o;
        return x == pixelId.x && y == pixelId.y && Objects.equals(room, pixelId.room);
    }

    @Override
    public int hashCode() {
        return Objects.hash(room, x, y);
    }

    public PixelId(UUID room, int x, int y) {
        this.x = x;
        this.y = y;
        this.room = room;
    }
}
