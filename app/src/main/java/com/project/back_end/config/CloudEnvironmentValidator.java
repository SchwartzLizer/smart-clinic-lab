package com.project.back_end.config;

import java.util.List;
import java.net.URI;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.project.back_end.config.properties.CorsProperties;

@Configuration(proxyBeanMethods = false)
@Profile("cloud")
public class CloudEnvironmentValidator {

    private static final List<String> REQUIRED = List.of(
            "DB_URL", "DB_USERNAME", "DB_PASSWORD",
            "FLYWAY_DB_URL", "FLYWAY_DB_USERNAME", "FLYWAY_DB_PASSWORD",
            "MONGODB_URI", "JWT_SECRET", "CORS_ALLOWED_ORIGINS",
            "APP_VERSION", "APP_REVISION", "APP_ENVIRONMENT");
    private static final List<String> REQUIRED_BOOTSTRAP = List.of(
            "APP_BOOTSTRAP_ADMIN_USERNAME", "APP_BOOTSTRAP_ADMIN_PASSWORD",
            "APP_BOOTSTRAP_DOCTOR_NAME", "APP_BOOTSTRAP_DOCTOR_SPECIALTY",
            "APP_BOOTSTRAP_DOCTOR_EMAIL", "APP_BOOTSTRAP_DOCTOR_PASSWORD",
            "APP_BOOTSTRAP_DOCTOR_PHONE", "APP_BOOTSTRAP_DOCTOR_AVAILABLE_TIMES");

    @Bean
    static BeanFactoryPostProcessor cloudConfigurationPreflight(Environment environment) {
        return beanFactory -> {
            List<String> invalid = REQUIRED.stream()
                    .filter(key -> invalid(key, environment.getProperty(key)))
                    .toList();
            String bootstrapEnabled = environment.getProperty("APP_BOOTSTRAP_ENABLED",
                    environment.getProperty("app.bootstrap.enabled", "false"));
            if (!isBoolean(bootstrapEnabled)) {
                invalid = append(invalid, "APP_BOOTSTRAP_ENABLED");
            } else if (Boolean.parseBoolean(bootstrapEnabled)) {
                invalid = java.util.stream.Stream.concat(invalid.stream(), REQUIRED_BOOTSTRAP.stream()
                        .filter(key -> invalid(key, environment.getProperty(key))))
                        .toList();
            }
            if (same(environment, "DB_USERNAME", "FLYWAY_DB_USERNAME")) invalid = append(invalid, "DB_USERNAME", "FLYWAY_DB_USERNAME");
            if (!invalid.isEmpty()) {
                throw new IllegalStateException("Cloud configuration requires: " + String.join(", ", invalid));
            }
        };
    }

    static boolean invalid(String key, String value) {
        if (value == null || value.isBlank()) return true;
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if ("CORS_ALLOWED_ORIGINS".equals(key)) {
            return java.util.Arrays.stream(value.split(","))
                    .map(String::trim)
                    .anyMatch(origin -> !CorsProperties.isExactOrigin(origin) || localHost(origin));
        }
        if ("JWT_SECRET".equals(key)) {
            return value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32
                    || normalized.contains("local") || normalized.contains("demo") || normalized.contains("test");
        }
        return normalized.contains("local-only") || normalized.contains("localhost")
                || normalized.equals("local") || normalized.equals("demo");
    }

    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean same(Environment environment, String first, String second) {
        String firstValue = environment.getProperty(first);
        String secondValue = environment.getProperty(second);
        return firstValue != null && secondValue != null && firstValue.trim().equals(secondValue.trim());
    }

    private static List<String> append(List<String> values, String... additions) {
        return java.util.stream.Stream.concat(values.stream(), java.util.Arrays.stream(additions)).distinct().toList();
    }

    private static boolean localHost(String originValue) {
        String host = URI.create(originValue).getHost().toLowerCase(java.util.Locale.ROOT)
                .replace("[", "").replace("]", "");
        return host.equals("localhost") || host.equals("::1") || host.startsWith("127.") || host.endsWith(".localhost");
    }
}
