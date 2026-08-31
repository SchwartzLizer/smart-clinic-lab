package com.project.back_end.DTO.auth;

import java.time.Instant;

import com.project.back_end.security.Role;

public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        Role role,
        Long accountId) {
}
