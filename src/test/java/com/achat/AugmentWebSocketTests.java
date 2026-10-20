package com.achat;

import com.achat.augment.AugmentType;
import com.achat.auth.JwtProvider;
import com.achat.message.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import java.lang.reflect.Type;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AugmentWebSocketTests extends AugmentTestSupport {
    @LocalServerPort int port;
    @Autowired com.achat.auth.AuthSessionService sessions;
    @Autowired ObjectMapper mapper;
    @Autowired SimpUserRegistry userRegistry;

    @Test void realWebSocketHandshakeRejectsForeignOrigin() {
        var client = new StandardWebSocketClient();
        var headers = new WebSocketHttpHeaders();
        headers.setOrigin("https://attacker.example");
        assertThatThrownBy(() -> client.execute(new org.springframework.web.socket.handler.AbstractWebSocketHandler() {},
                headers, java.net.URI.create("ws://127.0.0.1:" + port + "/ws")).get(5, TimeUnit.SECONDS))
                .hasStackTraceContaining("403");
    }

    @Test void realStompRejectsAnonymousAndRefreshConnect() throws Exception {
        for (String token : java.util.List.of("", sessions.issue(users.findById(ids.get(1)).orElseThrow()).refreshToken())) {
            var client = new WebSocketStompClient(new StandardWebSocketClient());
            var errors = new LinkedBlockingQueue<StompHeaders>();
            var headers = new StompHeaders();
            if (!token.isEmpty()) headers.add("Authorization", "Bearer " + token);
            try {
                var attempt = client.connectAsync("ws://127.0.0.1:" + port + "/ws", new WebSocketHttpHeaders(), headers,
                        new StompSessionHandlerAdapter() {
                            @Override public Type getPayloadType(StompHeaders h) { return byte[].class; }
                            @Override public void handleFrame(StompHeaders h, Object payload) { errors.add(h); }
                        });
                assertThat(errors.poll(5, TimeUnit.SECONDS)).isNotNull();
                attempt.cancel(true);
            } finally { client.stop(); }
        }
    }

    @Test void realStompChatReplyAndEffectUpdateMatchPersistedHistory() throws Exception {
        playing(AugmentType.TIME_BOMB);
        var client = new WebSocketStompClient(new StandardWebSocketClient());
        var converter = new MappingJackson2MessageConverter(); converter.setObjectMapper(mapper);
        client.setMessageConverter(converter);
        StompSession sender = null; StompSession respondent = null;
        try {
            sender = connect(client, ids.get(1));
            respondent = connect(client, ids.get(2));
            var incoming = new LinkedBlockingQueue<MessageResponse>();
            var updates = new LinkedBlockingQueue<MessageResponse>();
            sender.subscribe("/topic/rooms/" + roomId + "/messages", handler(incoming));
            sender.subscribe("/topic/rooms/" + roomId + "/message-effects", handler(updates));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (userRegistry.getUser(ids.get(1)).getSessions().stream()
                    .flatMap(session -> session.getSubscriptions().stream()).count() < 2 && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            sender.send("/app/rooms/" + roomId + "/chat", new SendMessageRequest("실제 소켓으로 전송한 시한폭탄 메시지", null, "socket-message"));
            var sent = incoming.poll(5, TimeUnit.SECONDS);
            assertThat(sent).isNotNull();
            assertThat(sent.augmentEffects().getFirst().type()).isEqualTo(AugmentType.TIME_BOMB);
            respondent.send("/app/rooms/" + roomId + "/chat", new SendMessageRequest("실제 소켓으로 전송한 답장입니다", sent.id(), "socket-reply"));
            var update = updates.poll(5, TimeUnit.SECONDS);
            assertThat(update).isNotNull();
            assertThat(update.id()).isEqualTo(sent.id());
            assertThat(update.augmentEffects().getFirst().status()).isEqualTo("SUCCESS");
            assertThat(update.augmentEffects().getFirst().points()).isEqualTo(10);
            assertThat(messages.findById(sent.id()).orElseThrow().getAugmentEffects()).isEqualTo(update.augmentEffects());
        } finally {
            if (sender != null && sender.isConnected()) sender.disconnect();
            if (respondent != null && respondent.isConnected()) respondent.disconnect();
            client.stop();
        }
    }
    private StompSession connect(WebSocketStompClient client, String userId) throws Exception {
        var headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + sessions.issue(users.findById(userId).orElseThrow()).accessToken());
        return client.connectAsync("ws://127.0.0.1:" + port + "/ws", new WebSocketHttpHeaders(), headers,
                new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    }
    private StompFrameHandler handler(BlockingQueue<MessageResponse> queue) {
        return new StompFrameHandler() {
            public Type getPayloadType(StompHeaders headers) { return MessageResponse.class; }
            public void handleFrame(StompHeaders headers, Object payload) { queue.add((MessageResponse) payload); }
        };
    }
}
