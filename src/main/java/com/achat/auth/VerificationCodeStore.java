package com.achat.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerificationCodeStore {

    private record CodeEntry(String code, Instant expiresAt) {}

    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final long expirationMs;

    public VerificationCodeStore(@Value("${app.verification.code-expiration-ms}") long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public void save(String email, String code) {
        Instant expiresAt = Instant.now().plusMillis(expirationMs);
        codes.put(email.toLowerCase(), new CodeEntry(code, expiresAt));
    }

    public Optional<String> getValidCode(String email) {
        CodeEntry entry = codes.get(email.toLowerCase());
        if (entry == null) return Optional.empty();
        if (Instant.now().isAfter(entry.expiresAt())) {
            codes.remove(email.toLowerCase());
            return Optional.empty();
        }
        return Optional.of(entry.code());
    }

    public void remove(String email) {
        codes.remove(email.toLowerCase());
    }
}