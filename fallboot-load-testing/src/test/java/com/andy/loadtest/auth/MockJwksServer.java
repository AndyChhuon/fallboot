package com.andy.loadtest.auth;

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
    }

    public void start() {
        app.start(9999);
    }

    public void stop() {
        app.stop();
    }
}
