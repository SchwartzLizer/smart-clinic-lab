package com.project.back_end.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.project.back_end.config.properties.JwtProperties;

class ConfigurationContractTests {

    @Test
    void rejectsJwtSecretShorterThanThirtyTwoUtf8Bytes() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtProperties("too-short", Duration.ofHours(1)));
    }

    @Test
    void rejectsNonPositiveJwtExpiration() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtProperties("12345678901234567890123456789012", Duration.ZERO));
    }

    @Test
    void acceptsValidJwtConfiguration() {
        assertDoesNotThrow(() -> new JwtProperties(
                "12345678901234567890123456789012", Duration.ofHours(1)));
    }
}
