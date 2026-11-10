package com.wzabek.rentengine.user.security;

import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        // Rzuca WeakKeyException przy sekrecie krótszym niż 32 bajty, więc aplikacja nie wstanie ze słabym kluczem
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = properties.expiration();
        this.clock = clock;
    }

    public String generateToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    // Sprawdza podpis i datę ważności; przy błędzie rzuca JwtException
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public AuthenticatedUser parseUser(String token) throws JwtException {
        Claims claims = parse(token);
        return new AuthenticatedUser(Long.valueOf(claims.getSubject()), claims.get("email", String.class),
                Role.valueOf(claims.get("role", String.class)));
    }

    public long expirationSeconds() {
        return expiration.toSeconds();
    }
}
