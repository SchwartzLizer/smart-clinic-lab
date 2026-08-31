package com.project.back_end.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.back_end.config.properties.CorsProperties;

class CorsPropertiesTests {

    @Test
    void acceptsOnlyAbsoluteExactHttpOriginsAndFailsClosedForPatterns() {
        CorsProperties properties = new CorsProperties(List.of(
                "https://app.example.com", "http://localhost:8080", "*", "https://example.com/path"));

        assertThat(properties.exactOrigins()).containsExactly("https://app.example.com", "http://localhost:8080");
        assertThat(CorsProperties.isExactOrigin("https://admin.example.com:8443")).isTrue();
        assertThat(CorsProperties.isExactOrigin("https://example.com?bad=true")).isFalse();
        assertThat(CorsProperties.isExactOrigin("https://user@example.com")).isFalse();
        assertThat(CorsProperties.isExactOrigin("*")).isFalse();
    }
}
