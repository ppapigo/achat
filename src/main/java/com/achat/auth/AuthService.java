package com.achat.auth;

import com.achat.auth.dto.*;
import com.achat.security.*;
import com.achat.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserStore userStore;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService sessions;
    private final RateLimitService limits;
    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validatePassword(request.password());
        if (userStore.existsByEmail(request.email())) throw new IllegalArgumentException("가입 정보를 확인해 주세요");
        User user = new User(request.email(), passwordEncoder.encode(request.password()));
        userStore.save(user);
        return new AuthResponse(user.getId(), user.getEmail(), "회원가입이 완료되었습니다");
    }
    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        limits.consume("login-account:" + email, 8, Duration.ofMinutes(15));
        User user = userStore.findByEmail(email).orElse(null);
        boolean matches = passwordEncoder.matches(request.password(), user == null ? DUMMY_HASH : user.getPasswordHash());
        if (user == null || !matches) throw new UnauthorizedException("이메일 또는 비밀번호가 틀렸습니다");
        return sessions.issue(user);
    }
    public TokenResponse refresh(String token) { return sessions.refresh(token); }
    public void logout(String token) { sessions.logout(token); }
    public UserInfoResponse getUserInfo(String userId) {
        return UserInfoResponse.from(userStore.findById(userId).orElseThrow(UnauthorizedException::new));
    }
    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        limits.consume("password:" + userId, 5, Duration.ofMinutes(15));
        User user = userStore.findById(userId).orElseThrow(UnauthorizedException::new);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) throw new UnauthorizedException("현재 비밀번호가 일치하지 않습니다");
        validatePassword(newPassword);
        user.changePassword(passwordEncoder.encode(newPassword));
    }
    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("비밀번호는 8자 이상, UTF-8 기준 72바이트 이하여야 합니다");
    }
}
