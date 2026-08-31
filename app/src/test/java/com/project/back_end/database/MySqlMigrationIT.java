package com.project.back_end.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MySqlMigrationIT {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("smart_clinic")
            .withUsername("smart_clinic")
            .withPassword("smart-clinic-local-only");

    @Container
    static final MySQLContainer<?> cloudMysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("smart_clinic_cloud")
            .withUsername("smart_clinic")
            .withPassword("smart-clinic-local-only");

    @Test
    void appliesSchemaProceduresAndHasHashedDemoPasswords() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();
        assertThat(flyway.info().applied())
                .extracting(info -> info.getVersion().getVersion())
                .containsExactlyInAnyOrder("1", "2", "3", "4");

        try (Connection connection = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                Statement statement = connection.createStatement()) {
            try (ResultSet tables = statement.executeQuery(
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE()")) {
                assertThat(resultSetValues(tables)).contains("admin", "doctor", "patient", "appointment");
            }
            try (ResultSet passwords = statement.executeQuery("SELECT password FROM admin")) {
                while (passwords.next()) {
                    assertThat(passwords.getString(1)).startsWith("$2");
                }
            }
        }
    }

    @Test
    void cloudTrackCreatesSchemaAndProceduresWithoutDemoRows() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(cloudMysql.getJdbcUrl(), cloudMysql.getUsername(), cloudMysql.getPassword())
                .locations("classpath:db/cloud-migration", "classpath:db/common-migration")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();

        flyway.migrate();
        assertThat(flyway.info().applied())
                .extracting(info -> info.getVersion().getVersion())
                .containsExactlyInAnyOrder("1", "2");
        try (Connection connection = DriverManager.getConnection(
                cloudMysql.getJdbcUrl(), cloudMysql.getUsername(), cloudMysql.getPassword());
                Statement statement = connection.createStatement()) {
            for (String table : java.util.List.of("admin", "doctor", "patient", "appointment")) {
                try (ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rows.next();
                    assertThat(rows.getInt(1)).isZero();
                }
            }
        }
    }

    private static java.util.List<String> resultSetValues(ResultSet resultSet) throws Exception {
        java.util.List<String> values = new java.util.ArrayList<>();
        while (resultSet.next()) {
            values.add(resultSet.getString(1));
        }
        return values;
    }
}
