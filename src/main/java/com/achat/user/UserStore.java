package com.achat.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserStore {

    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public UserStore(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        createAdminIfNotExists();
    }

    private void createAdminIfNotExists() {
        if (findByEmail("admin@achat.com").isEmpty()) {
            User admin = new User("admin@achat.com", passwordEncoder.encode("admin123!"));
            admin.setUsername("관리자");
            admin.setAdmin(true);
            admin.verifyEmail();
            save(admin);
            System.out.println("👑 기본 관리자 계정 생성: admin@achat.com / admin123!");
        }
    }

    public void save(User user) {
        usersById.put(user.getId(), user);
        usersByEmail.put(user.getEmail().toLowerCase(), user);
        usersByUsername.put(user.getUsername().toLowerCase(), user);
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email.toLowerCase()));
    }

    public Optional<User> findById(String id) {
        User user = usersById.get(id);
        return Optional.ofNullable(user);
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username.toLowerCase()));
    }

    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(email.toLowerCase());
    }

    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(username.toLowerCase());
    }

    public List<User> findAll() {
        return new ArrayList<>(usersById.values());
    }

    public int count() {
        return usersById.size();
    }

    public List<User> searchByUsername(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return usersById.values().stream()
                .filter(u -> u.getUsername().toLowerCase().contains(lowerKeyword))
                .toList();
    }
}