package com.project.back_end.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.project.back_end.DTO.doctor.DoctorCreateRequest;
import com.project.back_end.config.properties.BootstrapProperties;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;

import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class CloudIdentityBootstrapServiceTests {

    private static final String ADMIN_PASSWORD = "a".repeat(16);
    private static final String DOCTOR_PASSWORD = "b".repeat(16);

    @Mock private AdminRepository admins;
    @Mock private DoctorRepository doctors;
    @Mock private DoctorService doctorService;
    @Mock private PasswordEncoder passwords;
    @Mock private Validator validator;

    private CloudIdentityBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new CloudIdentityBootstrapService(admins, doctors, doctorService, passwords, validator);
    }

    @Test
    void createsBothMissingIdentitiesWithHashedAdminPassword() {
        when(admins.findAll()).thenReturn(List.of());
        when(doctors.findAll()).thenReturn(List.of());
        when(passwords.encode(ADMIN_PASSWORD)).thenReturn("$2a$encoded-admin-password");

        CloudIdentityBootstrapService.BootstrapResult result = service.bootstrap(properties());

        ArgumentCaptor<Admin> admin = ArgumentCaptor.forClass(Admin.class);
        ArgumentCaptor<DoctorCreateRequest> doctor = ArgumentCaptor.forClass(DoctorCreateRequest.class);
        verify(admins).save(admin.capture());
        verify(doctorService).createDoctor(doctor.capture());
        verify(admins).flush();
        verify(doctors).flush();
        assertThat(admin.getValue().getUsername()).isEqualTo("clinic.admin");
        assertThat(admin.getValue().getPassword()).isEqualTo("$2a$encoded-admin-password");
        assertThat(doctor.getValue().email()).isEqualTo("doctor@example.com");
        assertThat(doctor.getValue().availableTimes()).containsExactly("09:00-10:00", "13:00-14:00");
        assertThat(result.admin()).isEqualTo("created");
        assertThat(result.doctor()).isEqualTo("created");
    }

    @Test
    void serviceDoesNotEmitBootstrapSuccessLog() {
        when(admins.findAll()).thenReturn(List.of());
        when(doctors.findAll()).thenReturn(List.of());
        when(passwords.encode(ADMIN_PASSWORD)).thenReturn("$2a$encoded-admin-password");
        Logger logger = (Logger) LoggerFactory.getLogger(CloudIdentityBootstrapService.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        logger.addAppender(events);
        events.start();

        try {
            service.bootstrap(properties());
            assertThat(events.list).isEmpty();
        } finally {
            logger.detachAppender(events);
            events.stop();
        }
    }

    @Test
    void exactRerunDoesNotWriteOrReencode() {
        Admin admin = admin("clinic.admin", "$2a$stored-admin");
        Doctor doctor = doctor("doctor@example.com", "$2a$stored-doctor", List.of("09:00-10:00", "13:00-14:00"));
        when(admins.findAll()).thenReturn(List.of(admin));
        when(doctors.findAll()).thenReturn(List.of(doctor));
        when(passwords.matches(ADMIN_PASSWORD, admin.getPassword())).thenReturn(true);
        when(passwords.matches(DOCTOR_PASSWORD, doctor.getPassword())).thenReturn(true);

        CloudIdentityBootstrapService.BootstrapResult result = service.bootstrap(properties());

        verify(admins, never()).save(any());
        verify(doctorService, never()).createDoctor(any());
        verify(passwords, never()).encode(any());
        verify(admins, never()).flush();
        verify(doctors, never()).flush();
        assertThat(result.admin()).isEqualTo("existing");
        assertThat(result.doctor()).isEqualTo("existing");
    }

    @Test
    void matchingAdminOnlyCreatesOnlyDoctor() {
        Admin admin = admin("clinic.admin", "$2a$stored-admin");
        when(admins.findAll()).thenReturn(List.of(admin));
        when(doctors.findAll()).thenReturn(List.of());
        when(passwords.matches(ADMIN_PASSWORD, admin.getPassword())).thenReturn(true);

        CloudIdentityBootstrapService.BootstrapResult result = service.bootstrap(properties());

        verify(admins, never()).save(any());
        verify(doctorService).createDoctor(any());
        assertThat(result.admin()).isEqualTo("existing");
        assertThat(result.doctor()).isEqualTo("created");
    }

    @Test
    void matchingDoctorOnlyCreatesOnlyAdmin() {
        Doctor doctor = doctor("doctor@example.com", "$2a$stored-doctor", List.of("09:00-10:00", "13:00-14:00"));
        when(admins.findAll()).thenReturn(List.of());
        when(doctors.findAll()).thenReturn(List.of(doctor));
        when(passwords.matches(DOCTOR_PASSWORD, doctor.getPassword())).thenReturn(true);
        when(passwords.encode(ADMIN_PASSWORD)).thenReturn("$2a$encoded-admin-password");

        CloudIdentityBootstrapService.BootstrapResult result = service.bootstrap(properties());

        verify(admins).save(any());
        verify(doctorService, never()).createDoctor(any());
        assertThat(result.admin()).isEqualTo("created");
        assertThat(result.doctor()).isEqualTo("existing");
    }

    @Test
    void mismatchFailsWithoutWritesAndDoesNotEchoConfiguredValues() {
        Admin admin = admin("clinic.admin", "$2a$stored-admin");
        when(admins.findAll()).thenReturn(List.of(admin));
        when(passwords.matches(ADMIN_PASSWORD, admin.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> service.bootstrap(properties()))
                .isInstanceOf(CloudIdentityBootstrapService.BootstrapException.class)
                .hasMessageContaining("admin credentials")
                .hasMessageNotContaining("clinic.admin")
                .hasMessageNotContaining(ADMIN_PASSWORD)
                .hasMessageNotContaining(DOCTOR_PASSWORD);
        verify(admins, never()).save(any());
        verify(doctorService, never()).createDoctor(any());
    }

    @Test
    void invalidPasswordReuseFailsBeforeReadsOrWrites() {
        BootstrapProperties invalid = new BootstrapProperties(true, "clinic.admin", ADMIN_PASSWORD,
                "Dr. Bootstrap", "Cardiology", "doctor@example.com", ADMIN_PASSWORD,
                "0812345678", "09:00-10:00");

        assertThatThrownBy(() -> service.bootstrap(invalid))
                .isInstanceOf(CloudIdentityBootstrapService.BootstrapException.class)
                .hasMessageContaining("passwords");
        verify(admins, never()).findAll();
        verify(doctors, never()).findAll();
    }

    @Test
    void overlappingAvailabilityFailsBeforeReadsOrWrites() {
        BootstrapProperties invalid = new BootstrapProperties(true, "clinic.admin", ADMIN_PASSWORD,
                "Dr. Bootstrap", "Cardiology", "doctor@example.com", DOCTOR_PASSWORD,
                "0812345678", "09:00-10:00,09:30-10:30");

        assertThatThrownBy(() -> service.bootstrap(invalid))
                .isInstanceOf(CloudIdentityBootstrapService.BootstrapException.class)
                .hasMessageContaining("overlapping doctor availability");
        verify(admins, never()).findAll();
        verify(doctors, never()).findAll();
    }

    @Test
    void unrelatedDoctorFailsWithoutWrites() {
        when(admins.findAll()).thenReturn(List.of());
        when(doctors.findAll()).thenReturn(List.of(doctor("other@example.com", "$2a$other", List.of("09:00-10:00"))));

        assertThatThrownBy(() -> service.bootstrap(properties()))
                .isInstanceOf(CloudIdentityBootstrapService.BootstrapException.class)
                .hasMessageContaining("doctor state");
        verify(admins, never()).save(any());
        verify(doctorService, never()).createDoctor(any());
    }

    private static BootstrapProperties properties() {
        return new BootstrapProperties(true, " Clinic.Admin ", ADMIN_PASSWORD,
                " Dr. Bootstrap ", " Cardiology ", " DOCTOR@example.com ", DOCTOR_PASSWORD,
                "0812345678", "13:00-14:00,09:00-10:00");
    }

    private static Admin admin(String username, String password) {
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(password);
        return admin;
    }

    private static Doctor doctor(String email, String password, List<String> availability) {
        Doctor doctor = new Doctor();
        doctor.setName("Dr. Bootstrap");
        doctor.setSpecialty("Cardiology");
        doctor.setEmail(email);
        doctor.setPassword(password);
        doctor.setPhone("0812345678");
        doctor.setAvailableTimes(availability);
        return doctor;
    }
}
