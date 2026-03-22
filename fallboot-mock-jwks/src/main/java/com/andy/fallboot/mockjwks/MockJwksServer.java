package com.andy.fallboot.mockjwks;

import com.nimbusds.jose.jwk.JWKSet;
import io.javalin.Javalin;

public class MockJwksServer {

    private final Javalin app;

    public MockJwksServer(JwtTokenGenerator tokenGenerator) {
        this.app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()));
        });

        String jwksJson = new JWKSet(tokenGenerator.getPublicJwk()).toString();

        String openIdConfig = """
                {
                  "issuer": "%s",
                  "jwks_uri": "%s/.well-known/jwks.json",
                  "authorization_endpoint": "%s/authorize",
                  "token_endpoint": "%s/token",
                  "subject_types_supported": ["public"],
                  "id_token_signing_alg_values_supported": ["RS256"],
                  "response_types_supported": ["code"]
                }
                """.formatted(
                tokenGenerator.getIssuer(),
                tokenGenerator.getIssuer(),
                tokenGenerator.getIssuer(),
                tokenGenerator.getIssuer()
        );

        app.get("/.well-known/jwks.json", ctx -> {
            ctx.contentType("application/json");
            ctx.result(jwksJson);
        });

        app.get("/.well-known/openid-configuration", ctx -> {
            ctx.contentType("application/json");
            ctx.result(openIdConfig);
        });

        app.get("/token", ctx -> {
            String userIndexParam = ctx.queryParam("userIndex");
            int userIndex = Integer.parseInt(userIndexParam != null ? userIndexParam : "0");
            String token = tokenGenerator.generateToken(userIndex);
            ctx.contentType("application/json");
            ctx.result("{\"access_token\":\"" + token + "\"}");
        });

        // Batch endpoint: /tokens?start=0&count=500 — returns one token per line
        app.get("/tokens", ctx -> {
            String startParam = ctx.queryParam("start");
            String countParam = ctx.queryParam("count");
            int start = Integer.parseInt(startParam != null ? startParam : "0");
            int count = Integer.parseInt(countParam != null ? countParam : "100");
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < start + count; i++) {
                sb.append(tokenGenerator.generateToken(i)).append("\n");
            }
            ctx.contentType("text/plain");
            ctx.result(sb.toString());
        });
    }

    public void start(int port) {
        app.start(port);
    }

    public void stop() {
        app.stop();
    }
}
