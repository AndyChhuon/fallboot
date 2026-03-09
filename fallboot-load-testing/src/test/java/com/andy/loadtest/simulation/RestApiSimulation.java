package com.andy.loadtest.simulation;

import com.andy.loadtest.auth.JwtTokenGenerator;
import com.andy.loadtest.auth.MockJwksServer;
import com.andy.loadtest.config.LoadTestConfig;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class RestApiSimulation extends Simulation {

    private final JwtTokenGenerator tokenGenerator = new JwtTokenGenerator();
    private final MockJwksServer mockServer = new MockJwksServer(tokenGenerator);
    private final AtomicInteger userCounter = new AtomicInteger(0);

    private final Supplier<Map<String, Object>> feederSupplier = () -> {
        int idx = userCounter.getAndIncrement();
        return Map.of(
                "token", tokenGenerator.generateToken(idx),
                "userIndex", idx
        );
    };

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(LoadTestConfig.BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private final ScenarioBuilder scenario = scenario("REST API Load Test")
            .exec(session -> {
                Map<String, Object> data = feederSupplier.get();
                return session.setAll(data);
            })
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
            );

    {
        setUp(
                scenario.injectOpen(rampUsers(LoadTestConfig.USERS).during(LoadTestConfig.RAMP_SECONDS))
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().mean().lt(500),
                        global().successfulRequests().percent().gt(95.0)
                );
    }

    @Override
    public void before() {
        mockServer.start();
        System.out.println("Mock JWKS server started on port 9999");
        System.out.println("Token endpoint: http://localhost:9999/token?userIndex=0");
        System.out.println("Press Enter to start the simulation...");
        try { System.in.read(); } catch (Exception ignored) {}
    }

    @Override
    public void after() {
        mockServer.stop();
        System.out.println("Mock JWKS server stopped");
    }
}
