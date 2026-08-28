package com.project.back_end.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full HTTP journey proving that the deployed API contract works across MySQL,
 * MongoDB, JWT authentication, and role/ownership checks.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=test",
        "app.demo-data.enabled=false"
})
@ActiveProfiles("test")
class SmartClinicApiIT {

    private static final String PASSWORD = "password";

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("smart_clinic")
            .withUsername("smart_clinic")
            .withPassword("smart-clinic-local-only");

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    void completesAuthenticatedClinicJourneyAndEnforcesOwnership() {
        String adminToken = loginAdmin();
        long doctorId = createDoctor(adminToken);

        String patientEmail = "patient.portfolio@example.com";
        registerPatient(patientEmail, "7771234568", "Portfolio Patient");
        String patientToken = loginUser("/api/auth/patients/login", patientEmail);

        ResponseEntity<JsonNode> filteredDoctors = exchange(HttpMethod.GET,
                "/api/doctors?name=Portfolio&specialty=General%20Medicine&period=AM&page=0&size=10", null, null);
        assertThat(filteredDoctors.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filteredDoctors.getBody().path("content").findValuesAsText("id"))
                .contains(String.valueOf(doctorId));

        LocalDate appointmentDate = LocalDate.now().plusDays(2);
        LocalDateTime firstSlot = appointmentDate.atTime(9, 0);
        LocalDateTime updatedSlot = appointmentDate.atTime(14, 0);
        ResponseEntity<JsonNode> createdAppointment = exchange(HttpMethod.POST, "/api/appointments", patientToken,
                Map.of("doctorId", doctorId, "appointmentTime", firstSlot.toString()));
        assertThat(createdAppointment.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long appointmentId = createdAppointment.getBody().path("id").asLong();
        assertThat(appointmentId).isPositive();

        ResponseEntity<JsonNode> updatedAppointment = exchange(HttpMethod.PUT,
                "/api/appointments/" + appointmentId, patientToken,
                Map.of("appointmentTime", updatedSlot.toString()));
        assertThat(updatedAppointment.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedAppointment.getBody().path("appointmentTime").asText()).isEqualTo(updatedSlot.toString());

        ResponseEntity<JsonNode> profile = exchange(HttpMethod.GET, "/api/patients/me", patientToken, null);
        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profile.getBody().path("email").asText()).isEqualTo(patientEmail);

        String doctorEmail = "doctor.portfolio@example.com";
        String doctorToken = loginUser("/api/auth/doctors/login", doctorEmail);
        ResponseEntity<JsonNode> doctorAppointments = exchange(HttpMethod.GET, "/api/appointments", doctorToken, null);
        assertThat(doctorAppointments.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(doctorAppointments.getBody().findValuesAsText("id"))
                .contains(String.valueOf(appointmentId));

        ResponseEntity<JsonNode> createdPrescription = exchange(HttpMethod.POST, "/api/prescriptions", doctorToken,
                Map.of("patientName", "Portfolio Patient", "appointmentId", appointmentId,
                        "medication", "Amoxicillin", "dosage", "500mg", "doctorNotes", "Take with food."));
        assertThat(createdPrescription.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createdPrescription.getBody().path("appointmentId").asLong()).isEqualTo(appointmentId);

        ResponseEntity<JsonNode> prescription = exchange(HttpMethod.GET,
                "/api/prescriptions/" + appointmentId, doctorToken, null);
        assertThat(prescription.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(prescription.getBody().path("medication").asText()).isEqualTo("Amoxicillin");

        String secondPatientEmail = "patient.other@example.com";
        registerPatient(secondPatientEmail, "7771234569", "Other Patient");
        String secondPatientToken = loginUser("/api/auth/patients/login", secondPatientEmail);

        ResponseEntity<JsonNode> secondPatientAppointments = exchange(HttpMethod.GET, "/api/appointments",
                secondPatientToken, null);
        assertThat(secondPatientAppointments.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondPatientAppointments.getBody().findValuesAsText("id"))
                .doesNotContain(String.valueOf(appointmentId));

        ResponseEntity<JsonNode> forbiddenPrescription = exchange(HttpMethod.GET,
                "/api/prescriptions/" + appointmentId, secondPatientToken, null);
        assertThat(forbiddenPrescription.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<JsonNode> forbiddenUpdate = exchange(HttpMethod.PUT,
                "/api/appointments/" + appointmentId, secondPatientToken,
                Map.of("appointmentTime", appointmentDate.atTime(10, 0).toString()));
        assertThat(forbiddenUpdate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String loginAdmin() {
        ResponseEntity<JsonNode> response = exchange(HttpMethod.POST, "/api/auth/admin/login", null,
                Map.of("username", "admin", "password", PASSWORD));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("token").asText();
    }

    private long createDoctor(String adminToken) {
        ResponseEntity<JsonNode> response = exchange(HttpMethod.POST, "/api/doctors", adminToken,
                Map.of("name", "Dr. Portfolio", "specialty", "General Medicine",
                        "email", "doctor.portfolio@example.com", "password", PASSWORD,
                        "phone", "7771234567", "availableTimes", List.of("09:00-10:00", "14:00-15:00")));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().path("id").asLong();
    }

    private void registerPatient(String email, String phone, String name) {
        ResponseEntity<JsonNode> response = exchange(HttpMethod.POST, "/api/patients", null,
                Map.of("name", name, "email", email, "password", PASSWORD, "phone", phone,
                        "address", "1 Portfolio Road"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private String loginUser(String path, String email) {
        ResponseEntity<JsonNode> response = exchange(HttpMethod.POST, path, null,
                Map.of("email", email, "password", PASSWORD));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("token").asText();
    }

    private ResponseEntity<JsonNode> exchange(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
