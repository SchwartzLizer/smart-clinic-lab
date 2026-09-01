package com.project.back_end.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.project.back_end.config.properties.BootstrapProperties;
import com.project.back_end.services.CloudIdentityBootstrapService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class CloudIdentityBootstrapConfigurationTests {

    @Test
    void disabledBootstrapCreatesNoRunner() {
        context(false, "cloud").run(context -> assertThat(context).doesNotHaveBean(ApplicationRunner.class));
    }

    @Test
    void enabledBootstrapOutsideCloudCreatesNoRunner() {
        context(true, "test").run(context -> assertThat(context).doesNotHaveBean(ApplicationRunner.class));
    }

    @Test
    void enabledCloudBootstrapCreatesRunner() {
        context(true, "cloud").run(context -> assertThat(context).hasSingleBean(ApplicationRunner.class));
    }

    @Test
    void serviceIsCloudOnly() {
        Profile profile = CloudIdentityBootstrapService.class.getAnnotation(Profile.class);
        assertThat(profile.value()).containsExactly("cloud");
    }

    @Test
    void bootstrapPropertiesRedactAllValues() {
        String rendered = new BootstrapProperties(true, "sentinel-user", "sentinel-password",
                "sentinel-name", "sentinel-specialty", "sentinel@example.test", "second-password",
                "0812345678", "09:00-10:00").toString();
        assertThat(rendered).contains("enabled=true", "REDACTED")
                .doesNotContain("sentinel", "0812345678", "09:00-10:00", "second-password");
    }

    @Test
    void runnerLogsSuccessOnlyAfterBootstrapReturns() {
        context(true, "cloud").run(context -> {
            CloudIdentityBootstrapService service = context.getBean(CloudIdentityBootstrapService.class);
            when(service.bootstrap(org.mockito.ArgumentMatchers.any())).thenReturn(
                    new CloudIdentityBootstrapService.BootstrapResult("created", "existing"));
            ListAppender<ILoggingEvent> events = attachLogCapture();
            try {
                context.getBean(ApplicationRunner.class).run(new org.springframework.boot.DefaultApplicationArguments());
                assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage)
                        .containsExactly("Cloud identity bootstrap completed: admin=created, doctor=existing");
            } catch (Exception exception) {
                throw new AssertionError(exception);
            } finally {
                events.stop();
            }
        });
    }

    @Test
    void runnerDoesNotLogSuccessWhenBootstrapThrows() {
        context(true, "cloud").run(context -> {
            CloudIdentityBootstrapService service = context.getBean(CloudIdentityBootstrapService.class);
            doThrow(new IllegalStateException("sanitized"))
                    .when(service).bootstrap(org.mockito.ArgumentMatchers.any());
            ListAppender<ILoggingEvent> events = attachLogCapture();
            try {
                org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> context.getBean(ApplicationRunner.class).run(new org.springframework.boot.DefaultApplicationArguments()))
                        .isInstanceOf(IllegalStateException.class);
                assertThat(events.list).isEmpty();
            } finally {
                events.stop();
            }
        });
    }

    private static ApplicationContextRunner context(boolean enabled, String profile) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withUserConfiguration(CloudIdentityBootstrapConfiguration.class, BootstrapServiceTestConfiguration.class)
                .withBean(BootstrapProperties.class, () -> new BootstrapProperties(enabled, "", "", "", "", "", "", "", ""))
                .withPropertyValues("app.bootstrap.enabled=" + enabled);
    }

    @Configuration(proxyBeanMethods = false)
    static class BootstrapServiceTestConfiguration {
        @Bean
        CloudIdentityBootstrapService bootstrapService() {
            return org.mockito.Mockito.mock(CloudIdentityBootstrapService.class);
        }
    }

    private static ListAppender<ILoggingEvent> attachLogCapture() {
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(CloudIdentityBootstrapConfiguration.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        return events;
    }
}
