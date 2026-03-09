package com.andy.loadtest.config;

public final class LoadTestConfig {

    private LoadTestConfig() {
    }

    public static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    public static final String WS_URL = System.getProperty("wsUrl", "ws://localhost:8080");
    public static final int USERS = Integer.getInteger("users", 100);
    public static final int RAMP_SECONDS = Integer.getInteger("rampSeconds", 30);
    public static final String ROOM_ID = System.getProperty("roomId", "c55c81a0-806c-4108-b393-500d88851d88");
    public static final int DURATION_SECONDS = Integer.getInteger("durationSeconds", 30);
    public static final int PAUSE_MILLIS = Integer.getInteger("pauseMillis", 500);
}
