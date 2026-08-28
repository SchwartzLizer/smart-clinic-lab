package com.project.back_end.services;

import java.time.Instant;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.DTO.auth.AdminLoginRequest;
import com.project.back_end.DTO.auth.AuthResponse;
import com.project.back_end.DTO.auth.UserLoginRequest;
import com.project.back_end.DTO.patient.PatientRegistrationRequest;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.security.Role;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private final AdminRepository admins;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(AdminRepository admins, DoctorRepository doctors, PatientRepository patients,
            PasswordEncoder passwordEncoder, TokenService tokens) {
        this.admins = admins;
        this.doctors = doctors;
        this.patients = patients;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    public AuthResponse authenticateAdmin(AdminLoginRequest request) {
        Admin admin = admins.findByUsername(request.username())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(this::invalidCredentials);
        return response(admin.getId(), admin.getUsername(), Role.ADMIN);
    }

    public AuthResponse authenticateDoctor(UserLoginRequest request) {
        Doctor doctor = doctors.findByEmail(request.email())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(this::invalidCredentials);
        return response(doctor.getId(), doctor.getEmail(), Role.DOCTOR);
    }

    public AuthResponse authenticatePatient(UserLoginRequest request) {
        Patient patient = patients.findByEmail(request.email())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(this::invalidCredentials);
        return response(patient.getId(), patient.getEmail(), Role.PATIENT);
    }

    @Transactional
    public void registerPatient(PatientRegistrationRequest request) {
        if (patients.existsByEmail(request.email()) || patients.existsByPhone(request.phone())) {
            throw new DuplicateRegistrationException("Email or phone already exists");
        }
        Patient patient = new Patient();
        patient.setName(request.name());
        patient.setEmail(request.email());
        patient.setPassword(passwordEncoder.encode(request.password()));
        patient.setPhone(request.phone());
        patient.setAddress(request.address());
        patients.save(patient);
    }

    private AuthResponse response(Long accountId, String subject, Role role) {
        String token = tokens.generateToken(accountId, subject, role);
        Instant expiresAt = tokens.expiresAt(token);
        return new AuthResponse(token, "Bearer", expiresAt, role, accountId);
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException(INVALID_CREDENTIALS);
    }
}
