package com.andy.fallboot.shared.pixelEntities;

import com.andy.fallboot.shared.roomEntities.Room;
import com.andy.fallboot.shared.userEntities.User;
import jakarta.persistence.*;

@Entity
@IdClass(PixelId.class)
public class Pixel {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    private Room room;

    @Id
    private int x;

    @Id
    private int y;

    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    private User lastUpdatedBy;

    protected Pixel() {}

    public Room getRoom() {
        return room;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getColor() {
        return color;
    }

    public User getLastUpdatedBy() {
        return lastUpdatedBy;
    }
}
