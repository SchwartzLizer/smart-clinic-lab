package com.project.back_end.config;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.project.back_end.DTO.auth.AdminLoginRequest;
import com.project.back_end.DTO.auth.UserLoginRequest;
import com.project.back_end.config.properties.BootstrapProperties;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.services.CloudIdentityBootstrapService;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.AuthService;

import jakarta.validation.Validator;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "app.demo-data.enabled=false", "app.mongo.index.enabled=false" })
@ActiveProfiles("test")
@Import(CloudIdentityBootstrapTransactionIT.TransactionalBootstrapTestConfiguration.class)
class CloudIdentityBootstrapTransactionIT {

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

    @Autowired private CloudIdentityBootstrapService bootstrapService;
    @Autowired private AdminRepository admins;
    @Autowired private DoctorRepository doctors;
    @Autowired private AppointmentRepository appointments;
    @Autowired private PatientRepository patients;
    @Autowired private AuthService auth;
    @Autowired private PasswordEncoder passwords;

    @BeforeEach
    void clearStores() {
        appointments.deleteAll();
        patients.deleteAll();
        doctors.deleteAll();
        admins.deleteAll();
    }

    @Test
    void realProductionPathCreatesHashesAuthenticatesAndIsIdempotent() {
        BootstrapProperties input = properties();
        bootstrapService.bootstrap(input);
        var admin = admins.findAll().get(0);
        var doctor = doctors.findByNameLike("Bootstrap").get(0);
        assertThat(admins.count()).isEqualTo(1);
        assertThat(doctors.count()).isEqualTo(1);
        assertThat(doctor.getAvailableTimes()).containsExactly("09:00-10:00");
        assertThat(admin.getPassword()).isNotEqualTo(input.adminPassword());
        assertThat(doctor.getPassword()).isNotEqualTo(input.doctorPassword());
        assertThat(passwords.matches(input.adminPassword(), admin.getPassword())).isTrue();
        assertThat(passwords.matches(input.doctorPassword(), doctor.getPassword())).isTrue();
        assertThat(auth.authenticateAdmin(new AdminLoginRequest("clinic.admin", input.adminPassword())).role().name())
                .isEqualTo("ADMIN");
        assertThat(auth.authenticateDoctor(new UserLoginRequest("doctor@example.test", input.doctorPassword())).role().name())
                .isEqualTo("DOCTOR");
        String adminHash = admin.getPassword();
        String doctorHash = doctor.getPassword();
        bootstrapService.bootstrap(input);
        assertThat(admins.count()).isEqualTo(1);
        assertThat(doctors.count()).isEqualTo(1);
        assertThat(admins.findAll().get(0).getPassword()).isEqualTo(adminHash);
        assertThat(doctors.findByNameLike("Bootstrap").get(0).getPassword()).isEqualTo(doctorHash);
    }

    @Test
    void realProductionPathSafelyCompletesMatchingPartialState() {
        BootstrapProperties input = properties();
        com.project.back_end.models.Admin admin = new com.project.back_end.models.Admin();
        admin.setUsername("clinic.admin");
        admin.setPassword(passwords.encode(input.adminPassword()));
        admins.save(admin);
        bootstrapService.bootstrap(input);
        assertThat(admins.count()).isEqualTo(1);
        assertThat(doctors.count()).isEqualTo(1);

        clearStores();
        com.project.back_end.models.Doctor doctor = new com.project.back_end.models.Doctor();
        doctor.setName("Dr. Bootstrap");
        doctor.setSpecialty("Cardiology");
        doctor.setEmail("doctor@example.test");
        doctor.setPassword(passwords.encode(input.doctorPassword()));
        doctor.setPhone("0812345678");
        doctor.setAvailableTimes(java.util.List.of("09:00-10:00"));
        doctors.save(doctor);
        bootstrapService.bootstrap(input);
        assertThat(admins.count()).isEqualTo(1);
        assertThat(doctors.count()).isEqualTo(1);
    }

    @Test
    void unrelatedIdentityFailsClosedWithoutWrites() {
        com.project.back_end.models.Doctor doctor = new com.project.back_end.models.Doctor();
        doctor.setName("Dr. Other");
        doctor.setSpecialty("Cardiology");
        doctor.setEmail("other@example.test");
        doctor.setPassword(passwords.encode("c".repeat(16)));
        doctor.setPhone("0812345679");
        doctor.setAvailableTimes(java.util.List.of("09:00-10:00"));
        doctors.save(doctor);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> bootstrapService.bootstrap(properties()))
                .isInstanceOf(CloudIdentityBootstrapService.BootstrapException.class);
        assertThat(admins.count()).isZero();
        assertThat(doctors.count()).isEqualTo(1);
    }

    private static BootstrapProperties properties() {
        return new BootstrapProperties(true, "clinic.admin", "a".repeat(16),
                "Dr. Bootstrap", "Cardiology", "doctor@example.test", "b".repeat(16),
                "0812345678", "09:00-10:00");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TransactionalBootstrapTestConfiguration {
        @Bean
        CloudIdentityBootstrapService cloudIdentityBootstrapService(AdminRepository admins, DoctorRepository doctors,
                DoctorService doctorService, PasswordEncoder passwords, Validator validator) {
            return new CloudIdentityBootstrapService(admins, doctors, doctorService, passwords, validator);
        }
    }
}
