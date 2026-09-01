package com.project.back_end.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.back_end.config.properties.BootstrapProperties;
import com.project.back_end.services.CloudIdentityBootstrapService;

@Configuration(proxyBeanMethods = false)
@Profile("cloud")
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class CloudIdentityBootstrapConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CloudIdentityBootstrapConfiguration.class);

    @Bean
    ApplicationRunner cloudIdentityBootstrapRunner(CloudIdentityBootstrapService bootstrapService,
            BootstrapProperties properties) {
        return new CloudIdentityBootstrapRunner(bootstrapService, properties);
    }

    private record CloudIdentityBootstrapRunner(CloudIdentityBootstrapService bootstrapService,
            BootstrapProperties properties) implements ApplicationRunner {

        @Override
        public void run(ApplicationArguments arguments) {
            CloudIdentityBootstrapService.BootstrapResult result = bootstrapService.bootstrap(properties);
            LOG.info("Cloud identity bootstrap completed: admin={}, doctor={}", result.admin(), result.doctor());
        }
    }
}
