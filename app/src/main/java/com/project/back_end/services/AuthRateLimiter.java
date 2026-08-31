package com.project.back_end.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class AuthRateLimiter {

    private static final int MAX_BUCKETS = 10_000;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);
    private static final Duration PROCESS_WINDOW = Duration.ofMinutes(1);

    private final Clock clock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AuthRateLimiter(Clock clinicClock) {
        this.clock = clinicClock;
    }

    public LoginReservation reserveLogin(String endpoint, String identifier) {
        String identityKey = identityKey("login", endpoint, identifier);
        reserve(identityKey, 5, LOGIN_WINDOW);
        LoginReservation reservation = new LoginReservation(identityKey);
        try {
            consume(processKey("login"), 60, PROCESS_WINDOW);
            return reservation;
        } catch (RuntimeException exception) {
            loginAborted(reservation);
            throw exception;
        }
    }

    public void loginSucceeded(LoginReservation reservation) {
        buckets.remove(reservation.identityKey);
    }

    public void loginFailed(LoginReservation reservation) {
        completeFailure(reservation.identityKey, LOGIN_WINDOW);
    }

    public void loginAborted(LoginReservation reservation) {
        buckets.computeIfPresent(reservation.identityKey, (ignored, bucket) -> {
            bucket.reservations--;
            return bucket.reservations == 0 && bucket.count == 0 ? null : bucket;
        });
    }

    public void beforeRegistration(String email, String phone) {
        String key = identityKey("registration", "patient", email + "\u0000" + phone);
        consume(key, 3, REGISTRATION_WINDOW);
        consume(processKey("registration"), 30, PROCESS_WINDOW);
    }

    int bucketCount() {
        cleanup(clock.instant());
        return buckets.size();
    }

    private void reserve(String key, int limit, Duration window) {
        Instant now = clock.instant();
        if (buckets.size() >= MAX_BUCKETS) cleanup(now);
        java.util.concurrent.atomic.AtomicLong retryAfter = new java.util.concurrent.atomic.AtomicLong();
        buckets.compute(key, (ignored, existing) -> {
            if (existing == null || !existing.expiresAt.isAfter(now)) return new Bucket(0, 1, now.plus(window));
            if (existing.count + existing.reservations >= limit) {
                retryAfter.set(Math.max(1, Duration.between(now, existing.expiresAt).toSeconds()));
                return existing;
            }
            existing.reservations++;
            return existing;
        });
        if (retryAfter.get() > 0) throw new RateLimitExceededException(retryAfter.get());
    }

    private void completeFailure(String key, Duration window) {
        Instant now = clock.instant();
        buckets.compute(key, (ignored, existing) -> {
            if (existing == null || !existing.expiresAt.isAfter(now)) return new Bucket(1, 0, now.plus(window));
            existing.reservations = Math.max(0, existing.reservations - 1);
            existing.count++;
            return existing;
        });
    }

    private void consume(String key, int limit, Duration window) {
        Instant now = clock.instant();
        if (buckets.size() >= MAX_BUCKETS) cleanup(now);
        java.util.concurrent.atomic.AtomicLong retryAfter = new java.util.concurrent.atomic.AtomicLong();
        buckets.compute(key, (ignored, existing) -> {
            if (existing == null || !existing.expiresAt.isAfter(now)) return new Bucket(1, 0, now.plus(window));
            if (existing.count >= limit) {
                retryAfter.set(Math.max(1, Duration.between(now, existing.expiresAt).toSeconds()));
                return existing;
            }
            existing.count++;
            return existing;
        });
        if (retryAfter.get() > 0) throw new RateLimitExceededException(retryAfter.get());
    }

    private void cleanup(Instant now) {
        buckets.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(now));
        if (buckets.size() >= MAX_BUCKETS) {
            buckets.entrySet().stream().limit(buckets.size() - MAX_BUCKETS + 1)
                    .forEach(entry -> buckets.remove(entry.getKey(), entry.getValue()));
        }
    }

    private String identityKey(String type, String endpoint, String identifier) {
        return type + ':' + endpoint + ':' + hash(normalize(identifier));
    }

    private String processKey(String type) {
        return type + ":process";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static final class LoginReservation {
        private final String identityKey;

        private LoginReservation(String identityKey) {
            this.identityKey = identityKey;
        }
    }

    private static final class Bucket {
        private int count;
        private int reservations;
        private final Instant expiresAt;

        private Bucket(int count, int reservations, Instant expiresAt) {
            this.count = count;
            this.reservations = reservations;
            this.expiresAt = expiresAt;
        }
    }
}
