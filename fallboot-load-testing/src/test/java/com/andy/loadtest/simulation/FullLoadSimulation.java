package com.andy.loadtest.simulation;

import com.andy.loadtest.auth.JwtTokenGenerator;
import com.andy.loadtest.auth.MockJwksServer;
import com.andy.loadtest.config.LoadTestConfig;
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
            .contentTypeHeader("application/json")
            .shareConnections();

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
            .exec(
                    ws("Connect WebSocket").connect("/ws")
            )
            .exec(
                    ws("Auth")
                            .sendText(session -> "{\"type\":\"auth\",\"token\":\"" + session.getString("token") + "\"}")
                            .await(10).on(
                                    ws.checkTextMessage("Authenticated")
                                            .check(regex("authenticated"))
                            )
            )
            .exec(
                    ws("Subscribe")
                            .sendText("{\"type\":\"subscribe\",\"roomId\":\"" + LoadTestConfig.ROOM_ID + "\"}")
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
                                    ws("Send pixel update")
                                            .sendText(session ->
                                                    "{\"type\":\"pixel\",\"roomId\":\"" + LoadTestConfig.ROOM_ID +
                                                    "\",\"x\":" + session.getString("expectedX") +
                                                    ",\"y\":" + session.getString("expectedY") +
                                                    ",\"color\":\"" + session.getString("expectedColor") + "\"}"
                                            )
                                            .await(20).on(
                                                    ws.checkTextMessage("Snapshot ping received")
                                                            .check(regex("\"type\":\"snapshot\""))
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
        for (int batch = 0; batch < count; batch += batchSize) {
            int batchCount = Math.min(batchSize, count - batch);
            for (int retry = 0; retry < 5 && !remoteTokens.containsKey(batch); retry++) {
                try {
                    var url = new java.net.URL(LoadTestConfig.MOCK_JWKS_URL + "/tokens?start=" + batch + "&count=" + batchCount);
                    var conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(30000);
                    if (conn.getResponseCode() == 200) {
                        String body = new String(conn.getInputStream().readAllBytes());
                        String[] tokens = body.strip().split("\\R");
                        for (int i = 0; i < tokens.length; i++) {
                            if (!tokens[i].isBlank()) {
                                remoteTokens.put(batch + i, tokens[i].strip());
                            }
                        }
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    if (retry == 4) {
                        System.err.println("Batch fetch failed at " + batch + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
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

        // Verify kafka properly saved pixels to DB (the snapshot REST endpoint now returns
        // a CDN URL, not pixel data — so we verify exclusively against the DB endpoint)
        String token = getToken(0);
        Pattern entryPattern = Pattern.compile("\"(\\d+):(\\d+)\":\"(#[0-9A-Fa-f]+)\"");
        try (HttpClient client = HttpClient.newHttpClient()) {
            int totalSent = lastPixelColor.size();
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
