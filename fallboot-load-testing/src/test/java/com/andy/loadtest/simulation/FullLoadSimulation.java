package com.andy.loadtest.simulation;

import com.andy.loadtest.auth.JwtTokenGenerator;
import com.andy.loadtest.auth.MockJwksServer;
import com.andy.loadtest.config.LoadTestConfig;
import com.andy.loadtest.util.StompFrameHelper;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class FullLoadSimulation extends Simulation {

    private final boolean isRemoteMode = LoadTestConfig.MOCK_JWKS_URL != null;
    private final JwtTokenGenerator tokenGenerator = isRemoteMode ? null : new JwtTokenGenerator();
    private final MockJwksServer mockServer = isRemoteMode ? null : new MockJwksServer(tokenGenerator);
    private final AtomicInteger userCounter = new AtomicInteger(0);
    private final ConcurrentHashMap<Integer, String> remoteTokens = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> lastPixelColor = new ConcurrentHashMap<>();

    private static final String[] COLORS = {
            "#FF0000", "#00FF00", "#0000FF", "#FFFF00",
            "#FF00FF", "#00FFFF", "#000000", "#FFFFFF"
    };

    private String getToken(int idx) {
        if (isRemoteMode) {
            return remoteTokens.get(idx);
        }
        return tokenGenerator.generateToken(idx);
    }

    private final Supplier<Map<String, Object>> feederSupplier = () -> {
        int idx = userCounter.getAndIncrement();
        return Map.of(
                "token", getToken(idx),
                "userIndex", idx
        );
    };

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(LoadTestConfig.BASE_URL)
            .wsBaseUrl(LoadTestConfig.WS_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private Map<String, Object> randomPixelData() {
        int x = ThreadLocalRandom.current().nextInt(0, 1000);
        int y = ThreadLocalRandom.current().nextInt(0, 1000);
        String color = COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)];
        return Map.of(
                "pixelPayload", "{\"x\":" + x + ",\"y\":" + y + ",\"color\":\"" + color + "\"}",
                "expectedX", String.valueOf(x),
                "expectedY", String.valueOf(y),
                "expectedColor", color
        );
    }

    private final ScenarioBuilder fullScenario = scenario("Full Load")
            .exec(session -> {
                Map<String, Object> data = feederSupplier.get();
                return session.setAll(data);
            })
            // Get all rooms and select a room (REST)
            .exec(
                    http("GET /api/rooms")
                            .get("/api/rooms")
                            .header("Authorization", "Bearer #{token}")
                            .check(status().is(200))
            )
            .pause(1)
            .exec(
                    http("GET /api/pixels/room/{roomId}")
                            .get("/api/pixels/room/" + LoadTestConfig.ROOM_ID)
                            .header("Authorization", "Bearer #{token}")
                            .check(status().is(200))
            )
            .pause(1)
            // Connect WebSocket and send pixel updates
            .exec(
                    ws("Connect WebSocket").connect("/ws")
            )
            .exec(session -> {
                String token = session.getString("token");
                return session.set("connectFrame", StompFrameHelper.connectFrame(token));
            })
            .exec(
                    ws("STOMP CONNECT")
                            .sendText("#{connectFrame}")
                            .await(10).on(
                                    ws.checkTextMessage("STOMP CONNECTED")
                                            .check(regex("CONNECTED"))
                            )
            )
            .exec(
                    ws("STOMP SUBSCRIBE")
                            .sendText(StompFrameHelper.subscribeFrame(
                                    "/topic/room." + LoadTestConfig.ROOM_ID, "sub-0"))
            )
            .pause(1)
            .during(LoadTestConfig.DURATION_SECONDS).on(
                    exec(session -> session.setAll(randomPixelData()))
                            .exec(session -> {
                                String coord = session.getString("expectedX") + ":" + session.getString("expectedY");
                                lastPixelColor.put(coord, session.getString("expectedColor"));
                                return session;
                            })
                            .exec(
                                    ws("STOMP SEND pixel update")
                                            .sendText(session ->
                                                    StompFrameHelper.sendFrame(
                                                            "/app/update-pixel/" + LoadTestConfig.ROOM_ID,
                                                            session.getString("pixelPayload")
                                                    )
                                            )
                                            .await(10).on(
                                                    ws.checkTextMessage("Pixel broadcast received")
                                                            .check(regex("\"x\":\\d+"))
                                            )
                            )
                            .pause(Duration.ofMillis(LoadTestConfig.PAUSE_MILLIS))
            )
            .exec(ws("Disconnect WebSocket").close());

    {
        setUp(
                fullScenario.injectOpen(rampUsers(LoadTestConfig.USERS).during(LoadTestConfig.RAMP_SECONDS))
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().mean().lt(1000),
                        global().successfulRequests().percent().gt(90.0)
                );
    }

    private void prefetchRemoteTokens() {
        int count = LoadTestConfig.USERS;
        int batchSize = 500;
        System.out.println("Fetching " + count + " tokens in batches of " + batchSize + " from " + LoadTestConfig.MOCK_JWKS_URL);
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        for (int batch = 0; batch < count; batch += batchSize) {
            int batchCount = Math.min(batchSize, count - batch);
            try {
                HttpResponse<String> resp = client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(LoadTestConfig.MOCK_JWKS_URL + "/tokens?start=" + batch + "&count=" + batchCount))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString()
                );
                if (resp.statusCode() == 200) {
                    String[] tokens = resp.body().strip().split("\\R");
                    for (int i = 0; i < tokens.length; i++) {
                        if (!tokens[i].isBlank()) {
                            remoteTokens.put(batch + i, tokens[i].strip());
                        }
                    }
                } else {
                    System.err.println("Batch fetch failed at " + batch + ": HTTP " + resp.statusCode());
                }
            } catch (Exception e) {
                System.err.println("Batch fetch error at " + batch + ": " + e.getMessage());
            }
            System.out.println("Prefetched " + remoteTokens.size() + "/" + count + " tokens");
        }
    }

    @Override
    public void before() {
        if (isRemoteMode) {
            System.out.println("Remote mode: using mock JWKS server at " + LoadTestConfig.MOCK_JWKS_URL);
            prefetchRemoteTokens();
        } else {
            mockServer.start();
            System.out.println("Mock JWKS server started on port 9999");
            System.out.println("Token endpoint: http://localhost:9999/token?userIndex=0");
        }
        System.out.println("Press Enter to start the simulation...");
        try {
            System.in.read();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void after() {
        if (!isRemoteMode) {
            mockServer.stop();
        }
        System.out.println("Mock JWKS server stopped");

        System.out.println("Press Enter to evaluate after Kafka is done processing...");
        new Scanner(System.in).nextLine();

        // Fetch final pixel state from server and verify every sent pixel (likely hits caffeine/redis)
        String token = getToken(0);
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LoadTestConfig.BASE_URL + "/api/pixels/room/" + LoadTestConfig.ROOM_ID))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("REST verification failed with status: " + response.statusCode());
                return;
            }

            String body = response.body();
            Pattern entryPattern = Pattern.compile("\"(\\d+):(\\d+)\":\"(#[0-9A-Fa-f]+)\"");
            Matcher matcher = entryPattern.matcher(body);
            Map<String, String> serverPixels = new ConcurrentHashMap<>();
            while (matcher.find()) {
                serverPixels.put(matcher.group(1) + ":" + matcher.group(2), matcher.group(3));
            }

            int totalSent = lastPixelColor.size();
            int verified = 0;
            int mismatch = 0;
            int notFound = 0;

            for (Map.Entry<String, String> entry : lastPixelColor.entrySet()) {
                String serverColor = serverPixels.get(entry.getKey());
                if (serverColor == null) {
                    notFound++;
                    if (notFound <= 10) {
                        System.out.println("[NOT FOUND] " + entry.getKey() + " expected=" + entry.getValue());
                    }
                } else if (!serverColor.equals(entry.getValue())) {
                    mismatch++;
                    if (mismatch <= 10) {
                        System.out.println("[MISMATCH] " + entry.getKey() + " expected=" + entry.getValue() + " actual=" + serverColor);
                    }
                } else {
                    verified++;
                }
            }

            System.out.println("=== Pixel Verification via REST ===");
            System.out.println("Unique coordinates sent: " + totalSent);
            System.out.println("Server pixel count:      " + serverPixels.size());
            System.out.println("Verified correct:        " + verified);
            System.out.println("Color mismatch:          " + mismatch);
            System.out.println("Not found on server:     " + notFound);
            double pct = totalSent > 0 ? (verified * 100.0 / totalSent) : 0;
            System.out.printf("Verification rate:       %.1f%%%n", pct);

            if (mismatch == 0 && notFound == 0) {
                System.out.println("SUCCESS: All " + totalSent + " pixels verified with correct values");
            } else {
                System.out.println("FAILURE: " + (mismatch + notFound) + " pixels failed verification");
            }

            // Verify kafka properly saved in DB
            HttpRequest dbRequest = HttpRequest.newBuilder()
                    .uri(URI.create(LoadTestConfig.BASE_URL + "/api/pixels/room/test-verification/" + LoadTestConfig.ROOM_ID))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> dbResponse = client.send(dbRequest, HttpResponse.BodyHandlers.ofString());

            if (dbResponse.statusCode() != 200) {
                System.out.println("DB verification failed with status: " + dbResponse.statusCode());
                return;
            }

            String dbBody = dbResponse.body();
            Matcher dbMatcher = entryPattern.matcher(dbBody);
            Map<String, String> dbPixels = new ConcurrentHashMap<>();
            while (dbMatcher.find()) {
                dbPixels.put(dbMatcher.group(1) + ":" + dbMatcher.group(2), dbMatcher.group(3));
            }

            int dbVerified = 0;
            int dbMismatch = 0;
            int dbNotFound = 0;

            for (Map.Entry<String, String> entry : lastPixelColor.entrySet()) {
                String dbColor = dbPixels.get(entry.getKey());
                if (dbColor == null) {
                    dbNotFound++;
                    if (dbNotFound <= 10) {
                        System.out.println("[DB NOT FOUND] " + entry.getKey() + " expected=" + entry.getValue());
                    }
                } else if (!dbColor.equals(entry.getValue())) {
                    dbMismatch++;
                    if (dbMismatch <= 10) {
                        System.out.println("[DB MISMATCH] " + entry.getKey() + " expected=" + entry.getValue() + " actual=" + dbColor);
                    }
                } else {
                    dbVerified++;
                }
            }

            System.out.println("=== Pixel Verification via DB ===");
            System.out.println("Unique coordinates sent: " + totalSent);
            System.out.println("DB pixel count:          " + dbPixels.size());
            System.out.println("Verified correct:        " + dbVerified);
            System.out.println("Color mismatch:          " + dbMismatch);
            System.out.println("Not found in DB:         " + dbNotFound);
            double dbPct = totalSent > 0 ? (dbVerified * 100.0 / totalSent) : 0;
            System.out.printf("Verification rate:       %.1f%%%n", dbPct);

            if (dbMismatch == 0 && dbNotFound == 0) {
                System.out.println("SUCCESS: All " + totalSent + " pixels verified in DB");
            } else {
                System.out.println("FAILURE: " + (dbMismatch + dbNotFound) + " pixels failed DB verification");
            }
        } catch (Exception e) {
            System.out.println("REST verification error: " + e.getMessage());
        }
    }
}
