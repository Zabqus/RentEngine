package com.wzabek.rentengine.user.security;

import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!";
    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("jan@test.pl", "hash", "Jan", "Kowalski", Role.CLIENT);
        ReflectionTestUtils.setField(user, "id", 42L);
    }

    @Test
    void tokenContainsUserIdEmailAndRole() {
        JwtService service = serviceAt(NOW);

        Claims claims = service.parse(service.generateToken(user));

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("jan@test.pl");
        assertThat(claims.get("role", String.class)).isEqualTo("CLIENT");
        assertThat(claims.getExpiration().toInstant()).isEqualTo(NOW.plus(Duration.ofHours(24)));
    }

    @Test
    void tokenIsValidJustBeforeExpiration() {
        String token = serviceAt(NOW).generateToken(user);

        assertThat(serviceAt(NOW.plus(Duration.ofHours(24)).minusSeconds(1)).parse(token).getSubject())
                .isEqualTo("42");
    }

    @Test
    void expiredTokenIsRejected() {
        String token = serviceAt(NOW).generateToken(user);

        assertThatThrownBy(() -> serviceAt(NOW.plus(Duration.ofHours(25))).parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = serviceAt(NOW).generateToken(user);
        String[] parts = token.split("\\.");
        // Podmieniony payload z rolą ADMIN, ale stary podpis
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"42\",\"role\":\"ADMIN\"}".getBytes());

        assertThatThrownBy(() -> serviceAt(NOW).parse(parts[0] + "." + forgedPayload + "." + parts[2]))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithOtherSecretIsRejected() {
        JwtService other = new JwtService(new JwtProperties("another-secret-that-is-also-32-bytes-long!!",
                Duration.ofHours(24)), Clock.fixed(NOW, ZoneOffset.UTC));
        String token = other.generateToken(user);

        assertThatThrownBy(() -> serviceAt(NOW).parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tooShortSecretFailsOnStartup() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", Duration.ofHours(1)), Clock.systemUTC()))
                .isInstanceOf(WeakKeyException.class);
    }

    private static JwtService serviceAt(Instant instant) {
        return new JwtService(new JwtProperties(SECRET, Duration.ofHours(24)), Clock.fixed(instant, ZoneOffset.UTC));
    }
}
