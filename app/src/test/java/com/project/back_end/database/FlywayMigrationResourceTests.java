package com.project.back_end.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

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

    @Test
    void cloudTrackContainsSchemaAndProceduresButNoDemoData() throws IOException {
        byte[] cloudSchema = new ClassPathResource("db/cloud-migration/V1__create_smart_clinic_schema.sql")
                .getInputStream().readAllBytes();
        byte[] cloudProcedures = new ClassPathResource("db/cloud-migration/V2__create_stored_procedures.sql")
                .getInputStream().readAllBytes();
        byte[] baseSchema = new ClassPathResource("db/migration/V1__create_smart_clinic_schema.sql")
                .getInputStream().readAllBytes();
        byte[] baseProcedures = new ClassPathResource("db/migration/V2__create_stored_procedures.sql")
                .getInputStream().readAllBytes();

        assertThat(cloudSchema).isEqualTo(baseSchema);
        assertThat(cloudProcedures).isEqualTo(baseProcedures);
        assertThat(new ClassPathResource("db/cloud-migration/V3__seed_demo_accounts.sql").exists()).isFalse();
        assertThat(new ClassPathResource("db/common-migration/README.md").exists()).isTrue();
    }

    @Test
    void profileLocationsResolveTestOnlyCommonV5ExactlyOnce() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        assertThat(countNamed(resolver, "classpath*:db/migration/**/*", "V5__topology_fixture.fixture")).isZero();
        assertThat(countNamed(resolver, "classpath*:db/cloud-migration/**/*", "V5__topology_fixture.fixture")).isZero();
        assertThat(countNamed(resolver, "classpath*:db/common-migration/**/*", "V5__topology_fixture.fixture"))
                .isEqualTo(1);
        assertThat(countNamed(resolver, "classpath*:db/migration/**/*.sql", "V1__create_smart_clinic_schema.sql"))
                .isEqualTo(1);
        assertThat(countNamed(resolver, "classpath*:db/cloud-migration/**/*.sql", "V1__create_smart_clinic_schema.sql"))
                .isEqualTo(1);
    }

    private static long countNamed(PathMatchingResourcePatternResolver resolver, String pattern, String filename)
            throws IOException {
        return java.util.Arrays.stream(resolver.getResources(pattern))
                .filter(resource -> filename.equals(resource.getFilename()))
                .count();
    }
}
