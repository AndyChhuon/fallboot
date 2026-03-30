package com.andy.fallboot.mockjwks;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates JWT tokens for load testing only.
 * Uses a fixed RSA key pair so tokens survive server restarts and
 * multiple instances serve the same JWKS.
 */
public class JwtTokenGenerator {

    private static final String KEY_ID = "loadtest-key-1";

    // Fixed RSA key pair for load testing — NOT for production use
    private static final String PRIVATE_KEY_PEM = """
            MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDZCuUxiNMw78LK
            FPzT6xUBCKKF6acIKFL7tY1CxNt4Tpi4yIMpy68/TQJ8Pjra7gWREI8F28Ah+8zv
            dMStrio5r+IRQMsW69FwomU6nR71blPcBIJ0V0hgG/ClokTP2MVWlGnpvsODFufQ
            JGA8lquVUJMgE+hoXQd6v4iAcU/n/qYJ1SraMVTfotZoSgStJG/7An5um8UoEZgi
            5vAzOsB6vnkgU8ttTGNa+z82Ig2m5UegxGzYpk+4bWpaNJZC/LjptBfG27yfJukv
            YSEY+vdoImOlHQR8Yi6wHegcOB3fime5ajguWMhjBTj1u2QnAwLOoDgMo/zmcK5p
            T1GdNYPlAgMBAAECggEABlqX4kGZwdS/DINH8dyaXo+gvGVeSbJD2FEUu9fp/zq5
            JK21Y0pDzN6wXC/xMVARsyZmMtB1A+RXIp/LPd9mUTku19JLBRaDE4ukMC6shZha
            KvZ/Kts+8pYBee6HGljuUVjNGfXxoNCRRD9ltC21WQzFEIXCAm4jcNU9xdmAspoC
            G+3uRMBvM9b+3evQZtEzqpTIX2jeXehA+xbpKExUNOe2iw8b1EFnqaWBhBV5jupE
            +a0nS4nm4tUdIGllz3B1xSAC+032I2o1pRTxMh/eZKXFRMvbkqasVnCh/ZamuF/g
            YXEM9AvL0rQGimcaX1fyh6cogjNs2Z3R7dBWVTs5WQKBgQD6xaepmLXrPhPz2uce
            5jJ8nyBebbPLSv037t7k8crVTlmmqVMdY6PoRc+AfwcDOKDL2EAYPnIfA3o2Mum8
            9Dd8OfSiVMTO6GChvqCGiBDotsWIYbB6R1RnBMILldv5F4A0HESDcRS9vLwrSops
            Z1HP8/vTekLedJd1KuErC2DffQKBgQDdkTqzCOylj5t9KpbcCpTLkdGbnREltvf9
            857d5O/6qizVB8zjzn6Pjr/MmjgINd8ARBofYcTwwCiGu7WytknfqX6eceLJXsvr
            cd/6ko0kXLkO02nSbQ/1+jLfr8KtnL1MvAM3gC0kZA6C0f1ZOlQz1I6ZhL6riDsz
            caehCi2yiQKBgQDEtmGj/x/NLcAXDQM50NePvH92s1VTRjrGMoH0U9uJYdGfk7mY
            Fz5PCNwR7xNAKp6v6K22lt2MiHzIoT2LNIOF1iyZXieYKt8KYe+oOHoTIrRnHEKE
            WQnTWf8heWe5yP6PYhE4jm73u2JcDzfwe0fI+Zn0NTbZK9pXVdCVpHJUPQKBgQDD
            aPqkPBK6UdWIpq82kf55K7n5zKT65kwBpYNPoEImiT/RAngp0ky8v+FYygrw8tIi
            oe2ID+ppipAzhAnT/AFbSVlq1HZ3syuWE3C+xdHFaCGuebay541UG74SmijQhZRO
            wOL1aA5oMfa6WglfUJpCvAyoSPMAxb2wuWRmG1wCQQKBgFlqREpsJXCVumdMVLP0
            aCYZ4fXx1yJDW1zPzXdQeBtL6fA306R4Vf2q0nvIota8fEPUzX6s/Q3mDKkmI7/q
            ODX1i3AiKvIwaueI5DgbzuZo4U3o6A48pJnYd43Xae7dw2EIbZNwEN9FZbzZqbji
            C5+4elk7xXskrQqujrDJnXcC""";

    private static final String PUBLIC_KEY_PEM = """
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2QrlMYjTMO/CyhT80+sV
            AQiihemnCChS+7WNQsTbeE6YuMiDKcuvP00CfD462u4FkRCPBdvAIfvM73TEra4q
            Oa/iEUDLFuvRcKJlOp0e9W5T3ASCdFdIYBvwpaJEz9jFVpRp6b7Dgxbn0CRgPJar
            lVCTIBPoaF0Her+IgHFP5/6mCdUq2jFU36LWaEoErSRv+wJ+bpvFKBGYIubwMzrA
            er55IFPLbUxjWvs/NiINpuVHoMRs2KZPuG1qWjSWQvy46bQXxtu8nybpL2EhGPr3
            aCJjpR0EfGIusB3oHDgd34pnuWo4LljIYwU49btkJwMCzqA4DKP85nCuaU9RnTWD
            5QIDAQAB""";

    private final String issuer;
    private final RSAPrivateKey privateKey;
    private final RSAKey publicJwk;
    private final Map<Integer, String> tokenCache = new ConcurrentHashMap<>();

    public JwtTokenGenerator(String issuer) {
        this.issuer = issuer;
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");

            byte[] privateBytes = Base64.getMimeDecoder().decode(PRIVATE_KEY_PEM);
            this.privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));

            byte[] publicBytes = Base64.getMimeDecoder().decode(PUBLIC_KEY_PEM);
            RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(publicBytes));

            this.publicJwk = new RSAKey.Builder(publicKey)
                    .keyID(KEY_ID)
                    .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load fixed RSA key pair", e);
        }
    }

    public String generateToken(int userIndex) {
        return tokenCache.computeIfAbsent(userIndex, i -> {
            try {
                JWTClaimsSet claims = new JWTClaimsSet.Builder()
                        .subject("loadtest-user-" + i)
                        .issuer(issuer)
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
        return issuer;
    }
}
