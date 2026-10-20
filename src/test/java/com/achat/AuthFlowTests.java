package com.achat;

import com.achat.auth.AuthService;
import com.achat.auth.dto.LoginRequest;
import com.achat.auth.dto.RegisterRequest;
import com.achat.sanction.SanctionService;
import com.achat.sanction.SanctionType;
import com.achat.user.User;
import com.achat.user.UserStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTests {
    private static final String PASSWORD = "password123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AuthService auth;
    @Autowired UserStore users;
    @Autowired PasswordEncoder passwords;
    @Autowired SanctionService sanctions;
    @Autowired JdbcTemplate jdbc;

    private String email() {
        return UUID.randomUUID() + "@example.com";
    }

    @Test
    void registrationImmediatelyAllowsLoginAndAuthenticatedRequests() throws Exception {
        String email = email();
        String registration = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.message").value("회원가입이 완료되었습니다"))
                .andExpect(jsonPath("$.data.emailVerified").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String userId = mapper.readTree(registration).path("data").path("userId").asText();
        User stored = users.findById(userId).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, stored.getPasswordHash())).isTrue();

        String login = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email.toUpperCase(Locale.ROOT), PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).path("data").path("accessToken").asText();
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId));
    }

    @Test
    void existingUnverifiedAccountCanLogIn() throws Exception {
        User user = new User(email(), passwords.encode(PASSWORD));
        users.save(user);
        jdbc.update("update users set email_verified = ? where id = ?", false, user.getId());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(user.getEmail(), PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(user.getId()));
    }

    @Test
    void duplicateEmailAndWrongPasswordAreStillRejected() throws Exception {
        String email = email();
        auth.register(new RegisterRequest(email, PASSWORD));
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RegisterRequest(email.toUpperCase(Locale.ROOT), PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("가입 정보를 확인해 주세요"));
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("이메일 또는 비밀번호가 틀렸습니다"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"email\":\"invalid\",\"password\":\"password123!\"}",
            "{\"email\":\"valid@example.com\",\"password\":\"short\"}"
    })
    void registrationStillValidatesEmailAndPassword(String body) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @ParameterizedTest
    @EnumSource(value = SanctionType.class, names = {"TEMP_BAN", "PERM_BAN"})
    void bannedAccountsAreStillRejected(SanctionType type) throws Exception {
        String email = email();
        String userId = auth.register(new RegisterRequest(email, PASSWORD)).userId();
        sanctions.applySanction(userId, type, "auth regression", null, "SYSTEM", 60);
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("정지")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/verify", "/auth/resend-code"})
    void verificationEndpointsNoLongerExist(String path) throws Exception {
        String email = email();
        auth.register(new RegisterRequest(email, PASSWORD));
        String token = auth.login(new LoginRequest(email, PASSWORD)).accessToken();
        mvc.perform(post(path).header("Authorization", "Bearer " + token)
                        .param("email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unused@example.com\",\"code\":\"123456\"}"))
                .andExpect(status().isNotFound());
    }
}
