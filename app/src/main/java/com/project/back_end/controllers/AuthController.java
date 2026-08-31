package com.project.back_end.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.auth.AdminLoginRequest;
import com.project.back_end.DTO.auth.AuthResponse;
import com.project.back_end.DTO.auth.UserLoginRequest;
import com.project.back_end.DTO.patient.PatientRegistrationRequest;
import com.project.back_end.services.AuthService;
import com.project.back_end.services.AuthRateLimiter;
import com.project.back_end.services.DuplicateRegistrationException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService auth;
    private final AuthRateLimiter rateLimiter;

    public AuthController(AuthService auth, AuthRateLimiter rateLimiter) {
        this.auth = auth;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/auth/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return login("/api/auth/admin/login", request.username(), () -> auth.authenticateAdmin(request));
    }

    @PostMapping("/auth/doctors/login")
    public ResponseEntity<AuthResponse> doctorLogin(@Valid @RequestBody UserLoginRequest request) {
        return login("/api/auth/doctors/login", request.email(), () -> auth.authenticateDoctor(request));
    }

    @PostMapping("/auth/patients/login")
    public ResponseEntity<AuthResponse> patientLogin(@Valid @RequestBody UserLoginRequest request) {
        return login("/api/auth/patients/login", request.email(), () -> auth.authenticatePatient(request));
    }

    @PostMapping("/patients")
    public ResponseEntity<Map<String, String>> registerPatient(
            @Valid @RequestBody PatientRegistrationRequest request) {
        rateLimiter.beforeRegistration(request.email(), request.phone());
        auth.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Signup successful"));
    }

    private ResponseEntity<AuthResponse> login(String endpoint, String identifier,
            java.util.function.Supplier<AuthResponse> authentication) {
        AuthRateLimiter.LoginReservation reservation = rateLimiter.reserveLogin(endpoint, identifier);
        try {
            AuthResponse response = authentication.get();
            rateLimiter.loginSucceeded(reservation);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException exception) {
            rateLimiter.loginFailed(reservation);
            throw exception;
        } catch (RuntimeException exception) {
            rateLimiter.loginAborted(reservation);
            throw exception;
        }
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ProblemDetail> invalidCredentials(BadCredentialsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        problem.setTitle("Unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(DuplicateRegistrationException.class)
    ResponseEntity<ProblemDetail> duplicateRegistration(DuplicateRegistrationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Duplicate registration");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
