package com.achat.websocket;

import com.achat.auth.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtProvider jwtProvider;

    public WebSocketAuthInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token == null || !token.startsWith("Bearer ")) {
                log.warn("WebSocket 연결 거부: 토큰 없음");
                throw new IllegalArgumentException("인증 토큰이 필요합니다");
            }

            token = token.substring(7);

            if (!jwtProvider.isValid(token)) {
                log.warn("WebSocket 연결 거부: 유효하지 않은 토큰");
                throw new IllegalArgumentException("유효하지 않은 토큰입니다");
            }

            if (!"access".equals(jwtProvider.getTokenType(token))) {
                log.warn("WebSocket 연결 거부: Access 토큰이 아님");
                throw new IllegalArgumentException("Access 토큰이 필요합니다");
            }

            String userId = jwtProvider.getUserId(token);
            accessor.setUser(new StompPrincipal(userId));
            log.info("WebSocket 연결 성공: {}", userId);
        }

        return message;
    }

    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}