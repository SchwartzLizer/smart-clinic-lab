package com.project.back_end.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClinicTimeConfig {

    @Bean
    ZoneId clinicZoneId(@Value("${app.time-zone:Asia/Bangkok}") String timeZone) {
        return ZoneId.of(timeZone);
    }

    @Bean
    Clock clinicClock(ZoneId clinicZoneId) {
        return Clock.system(clinicZoneId);
    }

    @Bean
    ValidationConfigurationCustomizer clinicValidationClockProvider(Clock clinicClock) {
        return configuration -> configuration.clockProvider(() -> clinicClock);
    }
}
