package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.project.back_end.DTO.auth.AdminLoginRequest;
import com.project.back_end.DTO.auth.AuthResponse;
import com.project.back_end.DTO.auth.UserLoginRequest;
import com.project.back_end.DTO.patient.PatientRegistrationRequest;
import com.project.back_end.config.properties.JwtProperties;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.security.Role;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private AdminRepository admins;

    @Mock
    private DoctorRepository doctors;

    @Mock
    private PatientRepository patients;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService auth;

    @BeforeEach
    void setUp() {
        TokenService tokens = new TokenService(
                new JwtProperties("test-signing-key-that-is-at-least-32-bytes-long", Duration.ofHours(1)),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        auth = new AuthService(admins, doctors, patients, passwordEncoder, tokens);
    }

    @Test
    void adminLoginUsesPasswordEncoderAndReturnsAdminClaims() {
        Admin admin = new Admin();
        admin.setId(11L);
        admin.setUsername("admin");
        admin.setPassword("$2a$10$stored");
        when(admins.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("secret", admin.getPassword())).thenReturn(true);
        AuthResponse response = auth.authenticateAdmin(new AdminLoginRequest("admin", "secret"));

        assertEquals("Bearer", response.tokenType());
        assertEquals(Role.ADMIN, response.role());
        assertEquals(11L, response.accountId());
        verify(passwordEncoder).matches("secret", admin.getPassword());
    }

    @Test
    void doctorLoginUsesPasswordEncoderAndReturnsDoctorClaims() {
        Doctor doctor = new Doctor();
        doctor.setId(12L);
        doctor.setEmail("doctor@example.com");
        doctor.setPassword("$2a$10$stored");
        when(doctors.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctor));
        when(passwordEncoder.matches("secret", doctor.getPassword())).thenReturn(true);
        AuthResponse response = auth.authenticateDoctor(new UserLoginRequest("doctor@example.com", "secret"));

        assertEquals(Role.DOCTOR, response.role());
        assertEquals(12L, response.accountId());
        verify(passwordEncoder).matches("secret", doctor.getPassword());
    }

    @Test
    void missingAccountAndWrongPasswordUseSameInvalidCredentialsException() {
        when(admins.findByUsername("missing")).thenReturn(Optional.empty());
        when(doctors.findByEmail("doctor@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> auth.authenticateAdmin(new AdminLoginRequest("missing", "secret")));
        assertThrows(BadCredentialsException.class,
                () -> auth.authenticateDoctor(new UserLoginRequest("doctor@example.com", "secret")));
    }

    @Test
    void patientRegistrationEncodesPasswordBeforeSaving() {
        when(patients.existsByEmail("patient@example.com")).thenReturn(false);
        when(patients.existsByPhone("0812345678")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("$2a$10$encoded");
        PatientRegistrationRequest request = new PatientRegistrationRequest(
                "Patient Name", "patient@example.com", "secret", "0812345678", "Bangkok");

        auth.registerPatient(request);

        ArgumentCaptor<com.project.back_end.models.Patient> captor = ArgumentCaptor.forClass(
                com.project.back_end.models.Patient.class);
        verify(patients).save(captor.capture());
        assertEquals("$2a$10$encoded", captor.getValue().getPassword());
        assertEquals("patient@example.com", captor.getValue().getEmail());
    }

    @Test
    void duplicatePatientEmailOrPhoneIsRejectedBeforeSave() {
        PatientRegistrationRequest request = new PatientRegistrationRequest(
                "Patient Name", "patient@example.com", "secret", "0812345678", "Bangkok");
        when(patients.existsByEmail(request.email())).thenReturn(true);

        assertThrows(DuplicateRegistrationException.class, () -> auth.registerPatient(request));

        when(patients.existsByEmail(request.email())).thenReturn(false);
        when(patients.existsByPhone(request.phone())).thenReturn(true);
        assertThrows(DuplicateRegistrationException.class, () -> auth.registerPatient(request));
    }
}
