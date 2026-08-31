package com.project.back_end.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(
        boolean enabled,
        String adminUsername,
        String adminPassword,
        String doctorName,
        String doctorSpecialty,
        String doctorEmail,
        String doctorPassword,
        String doctorPhone,
        String doctorAvailableTimes) {

    @Override
    public String toString() {
        return "BootstrapProperties[enabled=" + enabled + ", values=REDACTED]";
    }
}
