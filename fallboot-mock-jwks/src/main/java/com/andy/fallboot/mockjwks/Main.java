package com.andy.fallboot.mockjwks;

public class Main {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "9999"));
        String issuer = System.getenv().getOrDefault("ISSUER", "http://localhost:" + port);

        System.out.println("Starting Mock JWKS Server on port " + port);
        System.out.println("Issuer: " + issuer);

        JwtTokenGenerator tokenGenerator = new JwtTokenGenerator(issuer);
        MockJwksServer server = new MockJwksServer(tokenGenerator);
        server.start(port);

        System.out.println("Mock JWKS Server running");
        System.out.println("  JWKS:     " + issuer + "/.well-known/jwks.json");
        System.out.println("  OpenID:   " + issuer + "/.well-known/openid-configuration");
        System.out.println("  Token:    " + issuer + "/token?userIndex=0");
        System.out.println("  Batch:    " + issuer + "/tokens?count=100");
    }
}
