package com.achat.security;
import com.achat.user.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionAccountCheck implements ApplicationRunner {
    private final UserStore users;
    private final PasswordEncoder passwords;
    @Override public void run(ApplicationArguments arguments) {
        // The legacy bootstrap credential was public. Preserve the account and fail closed until rotated.
        users.findByEmail("admin@achat.com").filter(user -> passwords.matches("admin123!", user.getPasswordHash()))
                .ifPresent(user -> { throw new IllegalStateException("Rotate the legacy bootstrap account credential before production startup"); });
    }
}
