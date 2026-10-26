package com.parlour.api.service;

import com.parlour.api.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection for the password step: after N wrong passwords for an email, that email is locked
 * for a while. Unknown emails are counted too, so the response never reveals whether an account exists.
 * Kept in memory (fine for one server); use Redis or the database if the backend is ever scaled out.
 */
@Component
public class LoginThrottle {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private record Entry(int failures, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockDuration;

    public LoginThrottle(@Value("${app.security.login-max-failures}") int maxFailures,
                         @Value("${app.security.login-lock-minutes}") long lockMinutes) {
        this.maxFailures = maxFailures;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    /** Throws 429 while the account is locked. */
    public void assertNotLocked(String email) {
        Entry e = entries.get(key(email));
        if (e != null && e.lockedUntil() != null && e.lockedUntil().isAfter(Instant.now())) {
            long minutes = Math.max(1, Duration.between(Instant.now(), e.lockedUntil()).toMinutes() + 1);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Try again in " + minutes + " minute(s)");
        }
    }

    public void recordFailure(String email) {
        if (entries.size() > MAX_TRACKED_KEYS) {
            Instant now = Instant.now();
            entries.values().removeIf(e -> e.lockedUntil() == null || e.lockedUntil().isBefore(now));
        }
        entries.merge(key(email), new Entry(1, null), (old, ignored) -> {
            boolean wasLockExpired = old.lockedUntil() != null && old.lockedUntil().isBefore(Instant.now());
            int failures = wasLockExpired ? 1 : old.failures() + 1;
            Instant lockedUntil = failures >= maxFailures ? Instant.now().plus(lockDuration) : null;
            return new Entry(failures, lockedUntil);
        });
    }

    public void reset(String email) {
        entries.remove(key(email));
    }

    private static String key(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
