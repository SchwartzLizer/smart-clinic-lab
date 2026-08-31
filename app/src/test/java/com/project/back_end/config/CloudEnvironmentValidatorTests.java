package com.project.back_end.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

class CloudEnvironmentValidatorTests {

    @Test
    void cloudContextFailsEarlyForMissingRequiredConfigurationKeys() {
        new ApplicationContextRunner()
                .withUserConfiguration(CloudEnvironmentValidator.class)
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("cloud"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().getMessage()).contains(
                            "DB_URL", "JWT_SECRET", "CORS_ALLOWED_ORIGINS", "APP_VERSION", "APP_REVISION");
                });
    }

    @Test
    void rejectsUnsafeCloudValuesWithoutEchoingThem() {
        assertThat(CloudEnvironmentValidator.invalid("CORS_ALLOWED_ORIGINS", "*")).isTrue();
        assertThat(CloudEnvironmentValidator.invalid("CORS_ALLOWED_ORIGINS", "http://localhost:8080")).isTrue();
        assertThat(CloudEnvironmentValidator.invalid("CORS_ALLOWED_ORIGINS", "https://127.0.0.1")).isTrue();
        assertThat(CloudEnvironmentValidator.invalid("CORS_ALLOWED_ORIGINS", "https://example.com/path")).isTrue();
        assertThat(CloudEnvironmentValidator.invalid("CORS_ALLOWED_ORIGINS", "not-an-origin")).isTrue();
        assertThat(CloudEnvironmentValidator.invalid("CORS_ALLOWED_ORIGINS", "https://example.com?x=1")).isTrue();
        assertThat(CloudEnvironmentValidator.invalid("CORS_ALLOWED_ORIGINS", "https://user@example.com")).isTrue();
        assertThat(CloudEnvironmentValidator.invalid("CORS_ALLOWED_ORIGINS", "https://[::1]")).isTrue();
        assertThat(CloudEnvironmentValidator.invalid("JWT_SECRET", "too-short")).isTrue();
        assertThat(CloudEnvironmentValidator.invalid("JWT_SECRET", "local-demo-signing-key-that-is-long-enough")).isTrue();
    }

    @Test
    void cloudContextRejectsBlankValuesAndEqualDatabaseUsersWithoutLeakingValues() {
        new ApplicationContextRunner()
                .withUserConfiguration(CloudEnvironmentValidator.class)
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("cloud"))
                .withPropertyValues(
                        "DB_URL=jdbc:mysql://db.example.com:3306/clinic",
                        "DB_USERNAME=shared-account",
                        "DB_PASSWORD=   ",
                        "FLYWAY_DB_URL=jdbc:mysql://db.example.com:3306/clinic",
                        "FLYWAY_DB_USERNAME=shared-account",
                        "FLYWAY_DB_PASSWORD=strong-migration-password",
                        "MONGODB_URI=mongodb://mongo.example.com:27017/clinic",
                        "JWT_SECRET=production-signing-key-with-at-least-thirty-two-bytes",
                        "CORS_ALLOWED_ORIGINS=https://app.example.com",
                        "APP_VERSION=1.0.0", "APP_REVISION=abc123", "APP_ENVIRONMENT=production")
                .run(context -> {
                    assertThat(context).hasFailed();
                    String message = context.getStartupFailure().getMessage();
                    assertThat(message).contains("DB_PASSWORD", "DB_USERNAME", "FLYWAY_DB_USERNAME");
                    assertThat(message).doesNotContain("shared-account", "strong-migration-password");
                });
    }

    @Test
    void cloudContextAcceptsDistinctRuntimeAndMigrationAccounts() {
        new ApplicationContextRunner()
                .withUserConfiguration(CloudEnvironmentValidator.class)
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("cloud"))
                .withPropertyValues(
                        "DB_URL=jdbc:mysql://db.example.com:3306/clinic",
                        "DB_USERNAME=runtime_user", "DB_PASSWORD=strong-runtime-password",
                        "FLYWAY_DB_URL=jdbc:mysql://db.example.com:3306/clinic",
                        "FLYWAY_DB_USERNAME=migration_user", "FLYWAY_DB_PASSWORD=strong-migration-password",
                        "MONGODB_URI=mongodb://mongo.example.com:27017/clinic",
                        "JWT_SECRET=production-signing-key-with-at-least-thirty-two-bytes",
                        "CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com:8443",
                        "APP_VERSION=1.0.0", "APP_REVISION=abc123", "APP_ENVIRONMENT=production")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void acceptsMultipleExactOriginsAndDistinctStrongCloudCredentials() {
        assertThat(CloudEnvironmentValidator.invalid("CORS_ALLOWED_ORIGINS",
                "https://app.example.com,https://admin.example.com:8443")).isFalse();
        assertThat(CloudEnvironmentValidator.invalid("JWT_SECRET",
                "production-signing-key-with-at-least-thirty-two-bytes")).isFalse();
        MockEnvironment environment = validEnvironment();
        environment.setProperty("FLYWAY_DB_USERNAME", "migration_user");
        assertThat(environment.getProperty("DB_USERNAME")).isNotEqualTo(environment.getProperty("FLYWAY_DB_USERNAME"));
    }

    private static MockEnvironment validEnvironment() {
        return new MockEnvironment()
                .withProperty("DB_URL", "jdbc:mysql://db.example.com:3306/clinic")
                .withProperty("DB_USERNAME", "runtime_user")
                .withProperty("DB_PASSWORD", "strong-runtime-password")
                .withProperty("FLYWAY_DB_URL", "jdbc:mysql://db.example.com:3306/clinic")
                .withProperty("FLYWAY_DB_USERNAME", "migration_user")
                .withProperty("FLYWAY_DB_PASSWORD", "strong-migration-password")
                .withProperty("MONGODB_URI", "mongodb://mongo.example.com:27017/clinic")
                .withProperty("JWT_SECRET", "production-signing-key-with-at-least-thirty-two-bytes")
                .withProperty("CORS_ALLOWED_ORIGINS", "https://app.example.com")
                .withProperty("APP_VERSION", "1.0.0")
                .withProperty("APP_REVISION", "abc123")
                .withProperty("APP_ENVIRONMENT", "production");
    }
}
