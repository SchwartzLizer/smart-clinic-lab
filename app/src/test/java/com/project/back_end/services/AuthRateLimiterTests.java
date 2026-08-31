package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class AuthRateLimiterTests {

    @Test
    void loginLimitExpiresResetsAndSeparatesRoutes() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2030-01-01T00:00:00Z"));
        AuthRateLimiter limiter = new AuthRateLimiter(clock);
        for (int index = 0; index < 5; index++) {
            limiter.loginFailed(limiter.reserveLogin("admin", "name@example.com"));
        }
        assertThrows(RateLimitExceededException.class, () -> limiter.reserveLogin("admin", "name@example.com"));
        assertDoesNotThrow(() -> limiter.loginAborted(limiter.reserveLogin("doctor", "name@example.com")));
        clock.advanceSeconds(15 * 60);
        assertDoesNotThrow(() -> limiter.loginAborted(limiter.reserveLogin("admin", "name@example.com")));
        var reservation = limiter.reserveLogin("admin", "name@example.com");
        limiter.loginFailed(reservation);
        limiter.loginSucceeded(limiter.reserveLogin("admin", "name@example.com"));
        assertDoesNotThrow(() -> limiter.loginAborted(limiter.reserveLogin("admin", "name@example.com")));
    }

    @Test
    void registrationAndProcessLimitsAreBoundedAndConcurrent() throws InterruptedException {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2030-01-01T00:00:00Z"));
        AuthRateLimiter limiter = new AuthRateLimiter(clock);
        for (int index = 0; index < 3; index++) limiter.beforeRegistration("name@example.com", "0800000000");
        assertThrows(RateLimitExceededException.class,
                () -> limiter.beforeRegistration("name@example.com", "0800000000"));

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] threads = new Thread[64];
        for (int index = 0; index < threads.length; index++) {
            int unique = index;
            threads[index] = new Thread(() -> {
                try {
                    limiter.beforeRegistration("user" + unique + "@example.com", "08" + unique);
                } catch (Throwable exception) {
                    failure.compareAndSet(null, exception);
                }
            });
            threads[index].start();
        }
        for (Thread thread : threads) thread.join();
        assertThat(failure.get()).isInstanceOf(RateLimitExceededException.class);
        clock.advanceSeconds(60 * 60);
        assertDoesNotThrow(() -> limiter.beforeRegistration("name@example.com", "0800000000"));
        assertDoesNotThrow(() -> limiter.bucketCount());
    }

    @Test
    void concurrentFailedLoginsReserveAtMostFiveAuthenticationExecutions() throws InterruptedException {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2030-01-01T00:00:00Z"));
        AuthRateLimiter limiter = new AuthRateLimiter(clock);
        AtomicInteger executed = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        Thread[] threads = new Thread[24];
        for (int index = 0; index < threads.length; index++) {
            threads[index] = new Thread(() -> {
                try {
                    AuthRateLimiter.LoginReservation reservation = limiter.reserveLogin("patient", "same@example.com");
                    executed.incrementAndGet();
                    limiter.loginFailed(reservation);
                } catch (RateLimitExceededException exception) {
                    rejected.incrementAndGet();
                }
            });
            threads[index].start();
        }
        for (Thread thread : threads) thread.join();

        assertThat(executed.get()).isEqualTo(5);
        assertThat(rejected.get()).isEqualTo(19);
    }

    private static final class AdjustableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private AdjustableClock(Instant initial) {
            this.instant = new AtomicReference<>(initial);
        }

        void advanceSeconds(long seconds) {
            instant.updateAndGet(value -> value.plusSeconds(seconds));
        }

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return instant.get(); }
    }
}
