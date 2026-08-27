package com.project.back_end.services;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class TokenService {
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Value("${jwt.secret}")
    private String secret;

    public TokenService(AdminRepository adminRepository, DoctorRepository doctorRepository,
            PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /** Creates the HMAC signing key from the configured JWT secret. */
    public SecretKey getSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Generates a signed JWT whose subject is the user's email or username. */
    public String generateToken(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token subject cannot be blank");
        }
        try {
            Date issuedAt = new Date();
            Date expiresAt = new Date(issuedAt.getTime() + 7L * 24 * 60 * 60 * 1000);
            return Jwts.builder()
                    .subject(email)
                    .issuedAt(issuedAt)
                    .expiration(expiresAt)
                    .signWith(getSigningKey())
                    .compact();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to generate JWT token", exception);
        }
    }

    /** Verifies a token and returns its subject. */
    public String extractIdentifier(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be blank");
        }
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractEmail(String token) {
        return extractIdentifier(token);
    }

    /** Returns false for malformed, expired, unknown-user, or unsupported-role tokens. */
    public boolean validateToken(String token, String user) {
        try {
            String identifier = extractIdentifier(token);
            return switch (user.toLowerCase()) {
                case "admin" -> adminRepository.findByUsername(identifier) != null;
                case "doctor" -> doctorRepository.findByEmail(identifier) != null;
                case "patient" -> patientRepository.findByEmail(identifier) != null;
                default -> false;
            };
        } catch (JwtException | IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }
}
