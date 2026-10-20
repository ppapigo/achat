package com.achat;

import com.achat.auth.AuthService;
import com.achat.auth.dto.*;
import com.achat.security.UnauthorizedException;
import com.achat.user.UserStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Opt-in post-remediation checks. The original findings remain in the historical audit document. */
@SpringBootTest
class AuthSecurityAuditTests {
    @Autowired AuthService auth;
    @Autowired UserStore users;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    private String email() { return UUID.randomUUID() + "@audit.invalid"; }
    @Test void bootstrapAdministratorIsAbsent() { assertThat(users.findByEmail("admin@achat.com")).isEmpty(); }
    @Test void signupStillNeedsNoVerificationAndStarterCoinsAreUnchanged() {
        String email = email();
        String id = auth.register(new RegisterRequest(email, "audit-password123!")).userId();
        assertThat(auth.login(new LoginRequest(email, "audit-password123!")).accessToken()).isNotBlank();
        assertThat(users.findById(id).orElseThrow().getCoins()).isEqualTo(1000);
    }
    @Test void repeatedLoginFailuresAreLimited() {
        String email = email();
        for (int i = 0; i < 8; i++) assertThatThrownBy(() -> auth.login(new LoginRequest(email, "wrong"))).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> auth.login(new LoginRequest(email, "wrong"))).isInstanceOf(com.achat.security.RateLimitService.RateLimitedException.class);
    }
    @Test void passwordChangeInvalidatesOldRefresh() {
        String email = email();
        String id = auth.register(new RegisterRequest(email, "audit-password123!")).userId();
        var token = auth.login(new LoginRequest(email, "audit-password123!"));
        jdbc.update("update users set password_hash = ? where id = ?", passwords.encode("changed-password123!"), id);
        assertThatThrownBy(() -> auth.refresh(token.refreshToken())).isInstanceOf(UnauthorizedException.class);
    }
}
