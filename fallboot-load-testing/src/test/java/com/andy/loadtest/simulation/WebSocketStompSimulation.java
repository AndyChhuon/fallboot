package com.andy.loadtest.simulation;

import com.andy.loadtest.auth.JwtTokenGenerator;
import com.andy.loadtest.auth.MockJwksServer;
import com.andy.loadtest.config.LoadTestConfig;
import com.andy.loadtest.util.StompFrameHelper;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class WebSocketStompSimulation extends Simulation {

    private final JwtTokenGenerator tokenGenerator = new JwtTokenGenerator();
    private final MockJwksServer mockServer = new MockJwksServer(tokenGenerator);
    private final AtomicInteger userCounter = new AtomicInteger(0);

    private static final String[] COLORS = {
            "#FF0000", "#00FF00", "#0000FF", "#FFFF00",
            "#FF00FF", "#00FFFF", "#000000", "#FFFFFF"
    };

    private final Supplier<Map<String, Object>> feederSupplier = () -> {
        int idx = userCounter.getAndIncrement();
        return Map.of(
                "token", tokenGenerator.generateToken(idx),
                "userIndex", idx
        );
    };

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(LoadTestConfig.BASE_URL)
            .wsBaseUrl(LoadTestConfig.WS_URL);

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

    private final ScenarioBuilder scenario = scenario("WebSocket STOMP Load Test")
            .exec(session -> {
                Map<String, Object> data = feederSupplier.get();
                return session.setAll(data);
            })
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
                                    "/topic/room/" + LoadTestConfig.ROOM_ID, "sub-0"))
            )
            .pause(1)
            .during(LoadTestConfig.DURATION_SECONDS).on(
                    exec(session -> session.setAll(randomPixelData()))
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
                                                            .check(bodyString().transform(body -> {
                                                                int jsonStart = body.indexOf("{");
                                                                if (jsonStart < 0) return "";
                                                                String json = body.substring(jsonStart).trim();
                                                                if (json.endsWith("\u0000"))
                                                                    json = json.substring(0, json.length() - 1);
                                                                return json.replaceAll(",\"lastUpdatedBy\":\\d+", "");
                                                            }).is(session ->
                                                                    "{\"color\":\"" + session.getString("expectedColor")
                                                                            + "\",\"x\":" + session.getString("expectedX")
                                                                            + ",\"y\":" + session.getString("expectedY")
                                                                            + "}"
                                                            ))
                                            )
                            )
                            .pause(Duration.ofMillis(LoadTestConfig.PAUSE_MILLIS))
            )
            .exec(ws("Disconnect WebSocket").close());

    {
        setUp(
                scenario.injectOpen(rampUsers(LoadTestConfig.USERS).during(LoadTestConfig.RAMP_SECONDS))
        ).protocols(httpProtocol)
                .assertions(
                        global().successfulRequests().percent().gt(90.0)
                );
    }

    @Override
    public void before() {
        mockServer.start();
        System.out.println("Mock JWKS server started on port 9999");
        System.out.println("Token endpoint: http://localhost:9999/token?userIndex=0");
        System.out.println("Press Enter to start the simulation...");
        try {
            System.in.read();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void after() {
        mockServer.stop();
        System.out.println("Mock JWKS server stopped");
    }
}
