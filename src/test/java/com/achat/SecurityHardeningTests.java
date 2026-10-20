package com.achat;

import com.achat.auth.*;
import com.achat.auth.dto.*;
import com.achat.common.RoomExecution;
import com.achat.match.*;
import com.achat.message.ChatMessageService;
import com.achat.message.dto.SendMessageRequest;
import com.achat.room.*;
import com.achat.security.*;
import com.achat.sanction.*;
import com.achat.shop.ShopService;
import com.achat.user.*;
import com.achat.websocket.WebSocketAuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityHardeningTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService auth;
    @Autowired AuthSessionService sessions;
    @Autowired UserStore users;
    @Autowired RoomStore rooms;
    @Autowired RoomService roomService;
    @Autowired RoomExecution execution;
    @Autowired PasswordEncoder passwords;
    @Autowired SanctionService sanctions;
    @Autowired WebSocketAuthInterceptor websocket;
    @Autowired ChatMessageService chat;
    @Autowired ShopService shop;
    @Autowired MatchService matching;
    @Autowired Environment environment;
    User user;
    TokenResponse tokens;
    String roomId;

    @BeforeEach void account() {
        user = new User(UUID.randomUUID() + "@security.test", passwords.encode("password123!"));
        users.save(user);
        tokens = sessions.issue(user);
        Room room = new Room("security", user.getId(), 8);
        rooms.save(room); roomId = room.getId();
    }
    @Test void defaultAdministratorIsNotBootstrapped() {
        assertThat(users.findByEmail("admin@achat.com")).isEmpty();
    }
    @Test void forgedExpiredMissingAndRefreshTokensCannotAccessApi() throws Exception {
        for (String token : List.of(tokens.refreshToken(), signed(Instant.now().minusSeconds(2), false), signed(Instant.now().plusSeconds(60), true), "invalid")) {
            mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }
    private String signed(Instant expiration, boolean forged) {
        String secret = forged ? "different-signature-key-with-at-least-sixty-four-characters-123456789"
                : environment.getProperty("app.jwt.secret");
        return Jwts.builder().issuer("achat").subject(user.getId()).id(UUID.randomUUID().toString())
                .claim("type", "access").claim("sid", sessions.authenticate(tokens.accessToken()).sessionId())
                .issuedAt(Date.from(Instant.now().minusSeconds(60))).expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }
    @Test void productionRejectsWeakKeysAndDevelopmentOrigins() {
        MockEnvironment prod = new MockEnvironment(); prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> new JwtProvider("a".repeat(64), 900000, 604800000, prod)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new SecurityOrigins("http://localhost:5173", prod)).isInstanceOf(IllegalStateException.class);
        assertThat(new SecurityOrigins("https://achat.grooverapp.com", prod).values()).hasSize(1);
    }
    @Test void refreshRotatesAndReuseRevokesTheWholeFamily() {
        TokenResponse rotated = auth.refresh(tokens.refreshToken());
        assertThat(rotated.refreshToken()).isNotEqualTo(tokens.refreshToken());
        assertThat(sessions.authenticate(rotated.accessToken()).userId()).isEqualTo(user.getId());
        assertThatThrownBy(() -> auth.refresh(tokens.refreshToken())).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> auth.refresh(rotated.refreshToken())).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> sessions.authenticate(rotated.accessToken())).isInstanceOf(UnauthorizedException.class);
    }
    @Test void concurrentRefreshHasOneWinnerAndReuseRevokesFamily() throws Exception {
        var gate = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 2; i++) tasks.add(pool.submit(() -> {
                gate.await();
                try { auth.refresh(tokens.refreshToken()); return true; }
                catch (UnauthorizedException e) { return false; }
            }));
            gate.countDown();
            assertThat(List.of(tasks.get(0).get(), tasks.get(1).get())).containsExactlyInAnyOrder(true, false);
            assertThatThrownBy(() -> sessions.authenticate(tokens.accessToken())).isInstanceOf(UnauthorizedException.class);
        }
    }
    @Test void logoutAndPasswordChangeInvalidateAccessAndRefresh() throws Exception {
        mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RefreshRequest(tokens.refreshToken())))).andExpect(status().isOk());
        assertThatThrownBy(() -> sessions.authenticate(tokens.accessToken())).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> auth.refresh(tokens.refreshToken())).isInstanceOf(UnauthorizedException.class);
        TokenResponse other = auth.login(new LoginRequest(user.getEmail(), "password123!"));
        auth.changePassword(user.getId(), "password123!", "new-password123!");
        assertThatThrownBy(() -> sessions.authenticate(other.accessToken())).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> auth.refresh(other.refreshToken())).isInstanceOf(UnauthorizedException.class);
        assertThat(auth.login(new LoginRequest(user.getEmail(), "new-password123!")).accessToken()).isNotBlank();
    }
    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/reports", "/api/seasons/end", "/jobs/exp?amount=100"})
    void ordinaryUsersCannotReachAdministrativeOperations(String endpoint) throws Exception {
        var request = endpoint.startsWith("/api/admin") ? get(endpoint) : post(endpoint);
        mvc.perform(request.header("Authorization", "Bearer " + tokens.accessToken())).andExpect(status().isForbidden());
    }
    @Test void administratorRoleComesFromDatabase() throws Exception {
        execution.run("test-admin:" + user.getId(), () -> users.findById(user.getId()).orElseThrow().setAdmin(true));
        mvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + tokens.accessToken())).andExpect(status().isOk());
    }
    @Test void bannedAccountCannotUseExistingTokensOrMajorApis() throws Exception {
        sanctions.applySanction(user.getId(), SanctionType.TEMP_BAN, "security test", null, "SYSTEM", 60);
        for (String path : List.of("/auth/me", "/rooms", "/api/shop/inventory", "/match/status")) {
            mvc.perform(get(path).header("Authorization", "Bearer " + tokens.accessToken())).andExpect(status().isForbidden());
        }
        assertThatThrownBy(() -> auth.refresh(tokens.refreshToken())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> matching.joinQueue(user.getId(), 8)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void loginAccountFailuresAreLimitedAcrossIps() throws Exception {
        for (int i = 0; i < 8; i++) {
            final int ip = i;
            mvc.perform(post("/auth/login").with(r -> { r.setRemoteAddr("192.0.2." + ip); return r; })
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new LoginRequest(user.getEmail(), "wrong"))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(user.getEmail(), "wrong"))))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }
    @Test void signupLimitCannotBeBypassedBySpoofingProxyHeaders() throws Exception {
        int before = users.count();
        for (int i = 0; i < 6; i++) {
            var result = mvc.perform(post("/auth/register").with(r -> { r.setRemoteAddr("198.51.100.241"); return r; })
                    .header("X-Real-IP", "203.0.113." + i).header("X-Forwarded-For", "203.0.113." + i)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(new RegisterRequest(UUID.randomUUID() + "@signup.test", "password123!"))));
            result.andExpect(i < 5 ? status().isOk() : status().isTooManyRequests());
        }
        assertThat(users.count()).isEqualTo(before + 5);
    }
    @Test void absentVerificationRoutesStayInaccessibleUnderRepeatedAttempts() throws Exception {
        for (String endpoint : List.of("/auth/verify", "/auth/resend-code", "/auth/forgot-password", "/auth/reset-password")) {
            for (int i = 0; i < 3; i++) mvc.perform(post(endpoint).header("Authorization", "Bearer " + tokens.accessToken())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"123456\"}")).andExpect(status().isNotFound());
        }
    }
    @Test void oversizedBodyAndMalformedJsonDoNotExposeInternals() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("x".repeat(65537)))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{")).andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception"))));
    }
    @Test void anonymousExpiredAndRefreshStompConnectAreRejected() {
        for (String token : List.of("", tokens.refreshToken(), signed(Instant.now().minusSeconds(1), false))) {
            assertThatThrownBy(() -> connect(UUID.randomUUID().toString(), token)).isInstanceOf(UnauthorizedException.class);
        }
    }
    @Test void subscriptionsAndSendAreStrictlyScoped() {
        String socket = UUID.randomUUID().toString(); connect(socket, tokens.accessToken());
        for (String destination : List.of("/topic/rooms/" + UUID.randomUUID() + "/messages", "/topic/rooms/*/messages",
                "/queue/error", "/user/another-user/queue/error", "/topic/match/another-user", "/topic/rooms/" + roomId + "/messages/extra")) {
            assertThatThrownBy(() -> frame(socket, StompCommand.SUBSCRIBE, destination, UUID.randomUUID().toString())).isInstanceOf(AccessDeniedException.class);
        }
        assertThat(frame(socket, StompCommand.SUBSCRIBE, "/topic/rooms/" + roomId + "/messages", "room")).isNotNull();
        assertThat(frame(socket, StompCommand.SUBSCRIBE, "/user/queue/friends", "private")).isNotNull();
        assertThat(frame(socket, StompCommand.SUBSCRIBE, "/topic/match/" + user.getId(), "match")).isNotNull();
        assertThatThrownBy(() -> frame(socket, StompCommand.SEND, "/topic/rooms/" + roomId + "/messages", null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> frame(socket, StompCommand.SEND, "/app/rooms/" + UUID.randomUUID() + "/chat", null)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void leavingRevokesExistingSubscriptionAtDeliveryAndSend() {
        String socket = UUID.randomUUID().toString(); connect(socket, tokens.accessToken());
        frame(socket, StompCommand.SUBSCRIBE, "/topic/rooms/" + roomId + "/messages", "room");
        assertThat(deliver(socket, "room")).isNotNull();
        execution.run(roomId, () -> rooms.findById(roomId).orElseThrow().removeMember(user.getId()));
        assertThat(deliver(socket, "room")).isNull();
        assertThatThrownBy(() -> frame(socket, StompCommand.SEND, "/app/rooms/" + roomId + "/chat", null)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void banAndLogoutRevokeExistingSubscriptions() {
        String socket = UUID.randomUUID().toString(); connect(socket, tokens.accessToken());
        frame(socket, StompCommand.SUBSCRIBE, "/user/queue/error", "private");
        assertThat(deliver(socket, "private")).isNotNull();
        sanctions.applySanction(user.getId(), SanctionType.TEMP_BAN, "security", null, "SYSTEM", 10);
        assertThat(deliver(socket, "private")).isNull();
        assertThatThrownBy(() -> frame(socket, StompCommand.SEND, "/app/rooms/" + roomId + "/chat", null)).isInstanceOf(AccessDeniedException.class);
        sessions.logout(tokens.refreshToken());
        assertThat(deliver(socket, "private")).isNull();
    }
    @Test void serviceRejectsOutsidersMuteAndOversizedChat() {
        assertThatThrownBy(() -> chat.send(roomId, UUID.randomUUID().toString(), new SendMessageRequest("hello", null, null))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> chat.send(roomId, user.getId(), new SendMessageRequest("x".repeat(2001), null, null))).isInstanceOf(IllegalArgumentException.class);
        sanctions.applySanction(user.getId(), SanctionType.MUTE, "security", null, "SYSTEM", 10);
        assertThatThrownBy(() -> chat.send(roomId, user.getId(), new SendMessageRequest("hello", null, null))).isInstanceOf(RuntimeException.class);
    }
    @Test void privateRoomsCannotBeJoinedAndUnrelatedSubroomCannotBeDeleted() {
        Room privateRoom = new Room("private", "another", 8); privateRoom.setType(RoomType.MATCH); rooms.save(privateRoom);
        assertThatThrownBy(() -> roomService.joinRoom(privateRoom.getId(), user.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> roomService.getRoom(privateRoom.getId(), user.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> roomService.deleteSubRoom(roomId, privateRoom.getId(), user.getId())).isInstanceOf(AccessDeniedException.class);
        assertThat(rooms.findById(privateRoom.getId())).isPresent();
    }
    @Test void concurrentQueueJoinsCreateOneEntry() throws Exception {
        MatchQueue queue = new MatchQueue();
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<Void>>();
            for (int i = 0; i < 64; i++) tasks.add(() -> { queue.join(user.getId(), 8); return null; });
            for (var future : executor.invokeAll(tasks)) future.get();
        }
        assertThat(queue.getQueueSize()).isEqualTo(1);
        assertThat(queue.pollPlayers(12)).containsExactly(user.getId());
    }
    @Test void simultaneousMatchingAndRejoiningCannotAssignAUserTwice() throws Exception {
        var players = new ArrayList<String>();
        for (int i = 0; i < 6; i++) {
            User player = new User(UUID.randomUUID() + "@matching.test", "fixture-hash");
            users.save(player); players.add(player.getId());
        }
        try (var executor = Executors.newFixedThreadPool(8)) {
            var joins = new ArrayList<Callable<Void>>();
            for (String id : players) for (int i = 0; i < 4; i++) joins.add(() -> { matching.joinQueue(id, 6); return null; });
            for (var future : executor.invokeAll(joins)) future.get();
            var first = executor.submit(matching::processMatchmaking);
            var second = executor.submit(matching::processMatchmaking);
            first.get(); second.get();
            String assigned = matching.getMatchedRoom(players.getFirst());
            assertThat(assigned).isNotNull();
            assertThat(rooms.findById(assigned).orElseThrow().getMemberIds()).containsExactlyInAnyOrderElementsOf(players);
            for (String id : players) {
                assertThat(matching.getMatchedRoom(id)).isEqualTo(assigned);
                assertThatThrownBy(() -> matching.joinQueue(id, 12)).isInstanceOf(com.achat.augment.AugmentConflictException.class);
                assertThat(rooms.findAll().stream().filter(room -> room.isMatchRoom() && room.hasMember(id)).count()).isEqualTo(1);
            }
        } finally {
            for (String id : players) matching.leaveQueue(id);
        }
    }
    @Test void simultaneousEntitlementPurchaseChargesOnlyOnce() throws Exception {
        var gate = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> purchase = () -> {
                gate.await();
                try { return shop.purchase(user.getId(), "title_newbie").success(); }
                catch (org.springframework.dao.DataAccessException e) { return false; }
            };
            var first = executor.submit(purchase); var second = executor.submit(purchase); gate.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        }
        assertThat(users.findById(user.getId()).orElseThrow().getCoins()).isEqualTo(900);
        assertThat(shop.getInventory(user.getId())).hasSize(1);
    }
    private void connect(String socket, String token) {
        var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setSessionId(socket); headers.setNativeHeader("Authorization", "Bearer " + token); headers.setLeaveMutable(true);
        websocket.preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null);
    }
    private Message<?> frame(String socket, StompCommand command, String destination, String subscription) {
        var headers = StompHeaderAccessor.create(command);
        headers.setSessionId(socket); headers.setDestination(destination);
        if (subscription != null) headers.setSubscriptionId(subscription);
        headers.setUser(() -> "spoofed-user"); headers.setLeaveMutable(true);
        return websocket.preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null);
    }
    private Message<?> deliver(String socket, String subscription) {
        var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(socket); headers.setSubscriptionId(subscription);
        return websocket.outbound().preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null);
    }
}
