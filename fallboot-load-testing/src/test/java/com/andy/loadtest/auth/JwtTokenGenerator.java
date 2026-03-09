package com.andy.loadtest.auth;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JwtTokenGenerator {

    private static final String KEY_ID = "loadtest-key-1";
    private static final String ISSUER = "http://localhost:9999";

    private final RSAPrivateKey privateKey;
    private final RSAKey publicJwk;
    private final Map<Integer, String> tokenCache = new ConcurrentHashMap<>();

    public JwtTokenGenerator() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

            this.publicJwk = new RSAKey.Builder(publicKey)
                    .keyID(KEY_ID)
                    .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    public String generateToken(int userIndex) {
        return tokenCache.computeIfAbsent(userIndex, i -> {
            try {
                JWTClaimsSet claims = new JWTClaimsSet.Builder()
                        .subject("loadtest-user-" + i)
                        .issuer(ISSUER)
                        .claim("email", "loadtest" + i + "@test.com")
                        .claim("given_name", "LoadUser" + i)
                        .claim("token_use", "access")
                        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                        .issueTime(new Date())
                        .build();

                JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(KEY_ID)
                        .type(JOSEObjectType.JWT)
                        .build();

                SignedJWT signedJWT = new SignedJWT(header, claims);
                signedJWT.sign(new RSASSASigner(privateKey));

                return signedJWT.serialize();
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate JWT for user " + i, e);
            }
        });
    }

    public RSAKey getPublicJwk() {
        return publicJwk;
    }

    public String getIssuer() {
        return ISSUER;
    }
}
