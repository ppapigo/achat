package com.achat.websocket;

import com.achat.auth.AuthSessionService;
import com.achat.room.RoomStore;
import com.achat.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final AuthSessionService sessions;
    private final RoomStore rooms;
    private final RateLimitService limits;
    private static final Pattern SEND = Pattern.compile("/app/rooms/([0-9a-f-]{36})/(chat|like|join|leave)");
    private static final Pattern SUBSCRIBE = Pattern.compile("/topic/rooms/([0-9a-f-]{36})/(messages|message-effects|likes|events|round|round/end|round/highlights|augments|quest)");
    private final ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>();
    private record Connection(AuthSessionService.Identity identity, ConcurrentHashMap<String, String> subscriptions) {}

    @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null || headers.getCommand() == null) return message;
        StompCommand command = headers.getCommand();
        String sessionId = headers.getSessionId();
        if (sessionId == null) throw new UnauthorizedException();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            String bearer = headers.getFirstNativeHeader("Authorization");
            headers.removeNativeHeader("Authorization");
            if (bearer == null || !bearer.startsWith("Bearer ")) throw new UnauthorizedException();
            var identity = sessions.authenticate(bearer.substring(7));
            limits.consume("ws-connect:" + identity.userId(), 20, Duration.ofMinutes(1));
            if (connections.size() >= 5000 || connections.putIfAbsent(sessionId,
                    new Connection(identity, new ConcurrentHashMap<>())) != null) throw new AccessDeniedException("연결 한도를 초과했습니다");
            headers.setUser(identity);
            return message;
        }
        if (command == StompCommand.DISCONNECT) { connections.remove(sessionId); return message; }
        Connection connection = connections.get(sessionId);
        if (connection == null) throw new UnauthorizedException();
        sessions.validate(connection.identity());
        headers.setUser(connection.identity());
        if (command == StompCommand.UNSUBSCRIBE) {
            if (headers.getSubscriptionId() != null) connection.subscriptions().remove(headers.getSubscriptionId());
            return message;
        }
        if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) throw new AccessDeniedException("허용되지 않은 명령입니다");
        limits.consume("ws-frame:" + connection.identity().userId(), 90, Duration.ofSeconds(10));
        authorize(headers.getDestination(), command == StompCommand.SEND, connection.identity().userId());
        if (command == StompCommand.SUBSCRIBE) {
            String id = headers.getSubscriptionId();
            if (id == null || id.length() > 100 || connection.subscriptions().size() >= 32
                    || connection.subscriptions().putIfAbsent(id, headers.getDestination()) != null)
                throw new AccessDeniedException("유효하지 않은 구독입니다");
        }
        return message;
    }
    public void authorize(String destination, boolean send, String userId) {
        if (destination == null || destination.length() > 200) throw new AccessDeniedException("허용되지 않은 경로입니다");
        if (!send && (destination.matches("/user/queue/(error|cooldown|quests|friends)")
                || destination.equals("/topic/match/" + userId))) return;
        var matcher = (send ? SEND : SUBSCRIBE).matcher(destination);
        if (!matcher.matches()) throw new AccessDeniedException("허용되지 않은 경로입니다");
        var room = rooms.findById(matcher.group(1)).orElseThrow(() -> new AccessDeniedException("방 접근 권한이 없습니다"));
        if (!room.hasMember(userId)) throw new AccessDeniedException("방 참여자만 접근할 수 있습니다");
    }
    // Revalidate at delivery as well: leaving, logout, expiry and bans revoke existing subscriptions.
    public ChannelInterceptor outbound() {
        return new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) return message;
                String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
                String subscription = SimpMessageHeaderAccessor.getSubscriptionId(message.getHeaders());
                Connection connection = sessionId == null ? null : connections.get(sessionId);
                if (connection == null || subscription == null) return null;
                try {
                    sessions.validate(connection.identity());
                    authorize(connection.subscriptions().get(subscription), false, connection.identity().userId());
                    return message;
                } catch (UnauthorizedException | AccessDeniedException e) { return null; }
            }
        };
    }
    @EventListener public void disconnected(SessionDisconnectEvent event) { connections.remove(event.getSessionId()); }
}
