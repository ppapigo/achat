package com.achat;

import com.achat.augment.*;
import com.achat.auth.JwtProvider;
import com.achat.websocket.WebSocketAuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AugmentApiTests extends AugmentTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired com.achat.auth.AuthSessionService sessions;
    @Autowired WebSocketAuthInterceptor interceptor;

    @Test void occupiedRecipientIsHttp409AndMissingCycleIsRejected() throws Exception {
        pick(0, 2, AugmentType.CAPS_LOCK); offer(1, AugmentType.CAPS_LOCK);
        String token = sessions.issue(users.findById(ids.get(1)).orElseThrow()).accessToken();
        String endpoint = "/rooms/" + roomId + "/augments/pick";
        mvc.perform(post(endpoint).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("type", "CAPS_LOCK", "targetUserId", ids.get(2), "roundId", round().getRoundId()))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        assertThat(states.find(ids.get(1), roomId).orElseThrow().hasSelected()).isFalse();
        mvc.perform(post(endpoint).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("type", "CAPS_LOCK", "targetUserId", ids.get(3)))))
                .andExpect(status().isBadRequest());
    }

    @Test void clientsCannotForgeEffectBroadcastsOrSubscribeToAnotherRoom() {
        var forged = StompHeaderAccessor.create(StompCommand.SEND);
        forged.setUser(() -> ids.get(1)); forged.setDestination("/topic/rooms/" + roomId + "/message-effects");
        forged.setLeaveMutable(true);
        var forgedMessage = MessageBuilder.createMessage(new byte[0], forged.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(forgedMessage, null)).isInstanceOf(RuntimeException.class);
        var outsider = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        outsider.setUser(() -> "outsider"); outsider.setDestination("/topic/rooms/" + roomId + "/messages");
        outsider.setLeaveMutable(true);
        assertThatThrownBy(() -> interceptor.preSend(MessageBuilder.createMessage(new byte[0], outsider.getMessageHeaders()), null))
                .isInstanceOf(RuntimeException.class);
        var connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setSessionId("augment-api-session");
        connect.setNativeHeader("Authorization", "Bearer " + sessions.issue(users.findById(ids.get(1)).orElseThrow()).accessToken());
        connect.setLeaveMutable(true);
        interceptor.preSend(MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()), null);
        var member = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        member.setSessionId("augment-api-session"); member.setSubscriptionId("effects");
        member.setUser(() -> ids.get(1)); member.setDestination("/topic/rooms/" + roomId + "/message-effects"); member.setLeaveMutable(true);
        assertThat(interceptor.preSend(MessageBuilder.createMessage(new byte[0], member.getMessageHeaders()), null)).isNotNull();
    }
}
