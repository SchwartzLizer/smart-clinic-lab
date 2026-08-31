package com.project.back_end.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.ZoneId;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.ClassPathResource;

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

    @Test
    void baseConfigurationUsesDynamicPortAndDoesNotActivateDemo() throws Exception {
        String base = new String(new ClassPathResource("application.properties").getInputStream().readAllBytes());
        String demo = Files.readString(Path.of("src/main/resources/application-demo.properties"));

        org.junit.jupiter.api.Assertions.assertTrue(base.contains("server.port=${PORT:8080}"));
        org.junit.jupiter.api.Assertions.assertFalse(base.contains("spring.profiles.active="));
        org.junit.jupiter.api.Assertions.assertFalse(base.contains("smart-clinic-local-only"));
        org.junit.jupiter.api.Assertions.assertTrue(demo.contains("smart-clinic-local-only"));
        org.junit.jupiter.api.Assertions.assertTrue(demo.contains("app.demo-data.enabled=true"));
    }

    @Test
    void clinicClockUsesBangkokBusinessZone() {
        ClinicTimeConfig config = new ClinicTimeConfig();
        ZoneId zone = config.clinicZoneId("Asia/Bangkok");

        org.junit.jupiter.api.Assertions.assertEquals(ZoneId.of("Asia/Bangkok"), config.clinicClock(zone).getZone());
    }
}
