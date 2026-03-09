package com.andy.loadtest.util;

public final class StompFrameHelper {

    private static final char NULL_CHAR = '\0';

    private StompFrameHelper() {}

    public static String connectFrame(String token) {
        return "CONNECT\n" +
                "accept-version:1.2\n" +
                "host:localhost\n" +
                "Authorization:Bearer " + token + "\n" +
                "\n" + NULL_CHAR;
    }

    public static String subscribeFrame(String destination, String subscriptionId) {
        return "SUBSCRIBE\n" +
                "id:" + subscriptionId + "\n" +
                "destination:" + destination + "\n" +
                "\n" + NULL_CHAR;
    }

    public static String sendFrame(String destination, String jsonBody) {
        return "SEND\n" +
                "destination:" + destination + "\n" +
                "content-type:application/json\n" +
                "\n" +
                jsonBody + NULL_CHAR;
    }

    public static String disconnectFrame() {
        return "DISCONNECT\n" +
                "receipt:disconnect-receipt\n" +
                "\n" + NULL_CHAR;
    }
}
