package com.project.back_end.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.project.back_end.config.properties.JwtProperties;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.security.Role;

class TokenServiceTests {

    private static final String SECRET = "12345678901234567890123456789012";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void generatesAndParsesRoleAwareClaims() {
        TokenService service = serviceAt(NOW);

        String token = service.generateToken(42L, "doctor@example.com", Role.DOCTOR);

        assertThat(service.parse(token)).isEqualTo(
                new AuthenticatedUser(42L, "doctor@example.com", Role.DOCTOR));
        assertThat(service.isValid(token)).isTrue();
        assertThat(service.expiresAt(token)).isEqualTo(NOW.plus(Duration.ofHours(1)));
    }

    @Test
    void rejectsTamperedAndExpiredTokens() {
        TokenService service = serviceAt(NOW);
        String token = service.generateToken(42L, "doctor@example.com", Role.DOCTOR);

        assertThat(service.isValid(token.substring(0, token.length() - 1) + "x")).isFalse();
        assertThat(serviceAt(NOW.plus(Duration.ofHours(2))).isValid(token)).isFalse();
    }

    @Test
    void rejectsShortSecretsBeforeSigning() {
        assertThatThrownBy(() -> new TokenService(
                new JwtProperties("short", Duration.ofHours(1)),
                Clock.fixed(NOW, ZoneOffset.UTC)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static TokenService serviceAt(Instant instant) {
        return new TokenService(
                new JwtProperties(SECRET, Duration.ofHours(1)),
                Clock.fixed(instant, ZoneOffset.UTC));
    }
}
