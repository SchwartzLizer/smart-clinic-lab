package com.project.back_end.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FlywayMigrationResourceTests {

    @Test
    void shipsOrderedSchemaProcedureAndDemoMigrations() throws IOException {
        String[] paths = {
                "db/migration/V1__create_smart_clinic_schema.sql",
                "db/migration/V2__create_stored_procedures.sql",
                "db/migration/V3__seed_demo_accounts.sql"
        };

        for (String path : paths) {
            assertThat(new ClassPathResource(path).exists()).as(path).isTrue();
        }

        String seed = new String(new ClassPathResource(paths[2]).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertThat(seed).contains("$2a$10$").doesNotContain("pass12345");
    }
}
