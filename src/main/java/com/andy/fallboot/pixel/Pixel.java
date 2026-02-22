package com.andy.fallboot.pixel;

import com.andy.fallboot.room.Room;
import com.andy.fallboot.user.User;
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
}
