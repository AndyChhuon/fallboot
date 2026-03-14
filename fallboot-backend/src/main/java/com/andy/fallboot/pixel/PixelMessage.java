package com.andy.fallboot.pixel;

public class PixelMessage {
    private String color;
    private int x;
    private int y;
    private Long lastUpdatedBy;

    public PixelMessage() {}

    public PixelMessage(String color, int x, int y){
        this.color = color;
        this.x = x;
        this.y = y;
    }

    public String getColor() {
        return color;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Long getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(Long lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }
}
