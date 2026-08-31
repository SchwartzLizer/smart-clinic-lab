package com.project.back_end.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.project.back_end.DTO.doctor.DoctorCreateRequest;
import com.project.back_end.config.properties.BootstrapProperties;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.services.CloudIdentityBootstrapService;
import com.project.back_end.services.DoctorService;

import jakarta.validation.Validator;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "app.demo-data.enabled=false")
@ActiveProfiles("test")
@Import(CloudIdentityBootstrapRollbackIT.TransactionConfig.class)
class CloudIdentityBootstrapRollbackIT {
    @Container static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("smart_clinic").withUsername("smart_clinic").withPassword("smart-clinic-local-only");
    @Container static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }
    @Autowired private CloudIdentityBootstrapService bootstrap;
    @Autowired private AdminRepository admins;
    @Autowired private DoctorRepository doctors;
    @Autowired private AppointmentRepository appointments;
    @Autowired private PatientRepository patients;
    @MockitoBean private DoctorService doctorService;
    @BeforeEach void clear() { appointments.deleteAll(); patients.deleteAll(); doctors.deleteAll(); admins.deleteAll(); }
    @Test void rollsBackAdminAfterDoctorFailure() {
        doThrow(new IllegalStateException("forced doctor failure")).when(doctorService).createDoctor(any(DoctorCreateRequest.class));
        assertThatThrownBy(() -> bootstrap.bootstrap(input())).isInstanceOf(CloudIdentityBootstrapService.BootstrapException.class);
        assertThat(admins.count()).isZero(); assertThat(doctors.count()).isZero();
    }
    private static BootstrapProperties input() { return new BootstrapProperties(true, "clinic.admin", "a".repeat(16), "Dr. Bootstrap", "Cardiology", "doctor@example.test", "b".repeat(16), "0812345678", "09:00-10:00"); }
    @TestConfiguration(proxyBeanMethods = false) static class TransactionConfig {
        @Bean CloudIdentityBootstrapService bootstrap(AdminRepository admins, DoctorRepository doctors, DoctorService doctorService, PasswordEncoder passwords, Validator validator) {
            return new CloudIdentityBootstrapService(admins, doctors, doctorService, passwords, validator);
        }
    }
}
