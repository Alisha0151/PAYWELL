package com.paywell.security;

import com.paywell.config.PaywellProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Short-lived signed access tokens. Refresh tokens are opaque random strings (see AuthService), not JWTs. */
@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final Duration accessTtl;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public JwtService(PaywellProperties props, Clock clock) {
        String secret = props.jwt() == null ? null : props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("PAYWELL_JWT_SECRET must be set and at least 32 bytes long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = props.jwt().issuer();
        this.accessTtl = props.jwt().accessTtl();
        this.clock = clock;
    }

    public String createAccessToken(UUID userId, String role) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).requireIssuer(issuer).clock(() -> Date.from(Instant.now(clock)))
                    .build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthPrincipal(UUID.fromString(c.getSubject()), c.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long accessTtlSeconds() { return accessTtl.toSeconds(); }

    /** 256 bits from SecureRandom, URL-safe, no padding. */
    public String newOpaqueToken() {
        byte[] b = new byte[32];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
