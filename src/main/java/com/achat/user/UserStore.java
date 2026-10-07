package com.achat.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
@Component
@Transactional
@RequiredArgsConstructor
public class UserStore {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        if (findByEmail("admin@achat.com").isEmpty()) {
            User admin = new User("admin@achat.com", passwordEncoder.encode("admin123!"));
            admin.setUsername("관리자");
            admin.setAdmin(true);
            admin.verifyEmail();
            repository.save(admin);
        }
    }
    public void save(User user) { repository.save(user); }
    public Optional<User> findByEmail(String email) { return repository.findByEmail(normalize(email)); }
    public Optional<User> findById(String id) { return repository.findById(id); }
    public Optional<User> findByUsername(String username) { return repository.findByUsernameKey(normalize(username)); }
    public boolean existsByEmail(String email) { return repository.existsByEmail(normalize(email)); }
    public boolean existsByUsername(String username) { return repository.existsByUsernameKey(normalize(username)); }
    public List<User> findAll() { return repository.findAll(); }
    public int count() { return Math.toIntExact(repository.count()); }
    public List<User> searchByUsername(String keyword) { return repository.findByUsernameContainingIgnoreCase(keyword); }
    private String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT); }
}
