package com.project.back_end.config.properties;

import java.util.List;
import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public List<String> exactOrigins() {
        return allowedOrigins == null ? List.of()
                : allowedOrigins.stream().filter(CorsProperties::isExactOrigin).map(String::trim).toList();
    }

    public static boolean isExactOrigin(String value) {
        if (value == null || value.isBlank() || value.contains("*")) return false;
        try {
            URI origin = URI.create(value.trim());
            String rawPath = origin.getRawPath();
            return ("http".equalsIgnoreCase(origin.getScheme()) || "https".equalsIgnoreCase(origin.getScheme()))
                    && origin.getHost() != null && origin.getUserInfo() == null
                    && (rawPath == null || rawPath.isEmpty())
                    && origin.getRawQuery() == null && origin.getRawFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
