package com.project.back_end.services;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.project.back_end.config.properties.JwtProperties;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.security.Role;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class TokenService {

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    @Autowired
    public TokenService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public TokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }

    public String generateToken(Long accountId, String subject, Role role) {
        if (accountId == null || accountId < 0 || subject == null || subject.isBlank() || role == null) {
            throw new IllegalArgumentException("JWT account, subject, and role are required");
        }
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.expiration());
        return Jwts.builder()
                .subject(subject)
                .claim("accountId", accountId)
                .claim("role", role.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be blank");
        }
        var claims = Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number accountId = claims.get("accountId", Number.class);
        String subject = claims.getSubject();
        String roleName = claims.get("role", String.class);
        if (accountId == null || subject == null || subject.isBlank() || roleName == null) {
            throw new JwtException("Required JWT claims are missing");
        }
        try {
            return new AuthenticatedUser(accountId.longValue(), subject, Role.valueOf(roleName));
        } catch (IllegalArgumentException exception) {
            throw new JwtException("Unsupported JWT role", exception);
        }
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Instant expiresAt(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .toInstant();
    }

    /** Transitional adapter for course-era callers; removed when legacy controllers are deleted. */
    @Deprecated
    public String generateToken(String subject) {
        return generateToken(0L, subject, Role.PATIENT);
    }

    /** Transitional adapter for course-era callers; no repository lookup is performed. */
    @Deprecated
    public String extractIdentifier(String token) {
        return parse(token).subject();
    }

    /** Transitional adapter for course-era callers. */
    @Deprecated
    public String extractEmail(String token) {
        return extractIdentifier(token);
    }

    /** Transitional adapter for course-era callers; new endpoints use Spring Security authorities. */
    @Deprecated
    public boolean validateToken(String token, String user) {
        try {
            return parse(token).role().name().equalsIgnoreCase(user);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
