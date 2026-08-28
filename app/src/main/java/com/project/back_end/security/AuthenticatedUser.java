package com.project.back_end.security;

public record AuthenticatedUser(Long accountId, String subject, Role role) {
}
