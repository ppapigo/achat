package com.achat.websocket;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final com.achat.security.SecurityOrigins origins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue").setTaskScheduler(heartbeatScheduler())
                .setHeartbeatValue(new long[] {10_000, 10_000});
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");  // 이거 추가!
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins.values().toArray(String[]::new));
        registry.setErrorHandler(new org.springframework.web.socket.messaging.StompSubProtocolErrorHandler() {
            @Override public org.springframework.messaging.Message<byte[]> handleClientMessageProcessingError(
                    org.springframework.messaging.Message<byte[]> clientMessage, Throwable exception) {
                var headers = org.springframework.messaging.simp.stomp.StompHeaderAccessor.create(
                        org.springframework.messaging.simp.stomp.StompCommand.ERROR);
                headers.setMessage("실시간 요청이 거부되었습니다. 인증과 방 참여 상태를 확인해 주세요");
                return org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
            }
        });
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
        registration.taskExecutor().corePoolSize(2).maxPoolSize(8).queueCapacity(500);
    }
    @org.springframework.context.annotation.Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler heartbeatScheduler() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1); scheduler.setThreadNamePrefix("stomp-heartbeat-");
        return scheduler;
    }
    @org.springframework.context.annotation.Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2); scheduler.setThreadNamePrefix("app-scheduled-");
        return scheduler;
    }

    @Override public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor.outbound());
        registration.taskExecutor().corePoolSize(2).maxPoolSize(8).queueCapacity(500);
    }
    @Override public void configureWebSocketTransport(org.springframework.web.socket.config.annotation.WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(16 * 1024).setSendBufferSizeLimit(128 * 1024)
                .setSendTimeLimit(10_000).setTimeToFirstMessage(10_000);
    }
}
