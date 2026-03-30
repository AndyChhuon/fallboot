package com.andy.fallboot.shared.config;

import com.andy.fallboot.pixel.component.BroadcastSessionManager;
import com.andy.fallboot.shared.config.interceptor.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final BroadcastSessionManager broadcastSessionManager;
    private final Map<String, SimpleBrokerMessageHandler> brokerHandlers;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
                           BroadcastSessionManager broadcastSessionManager,
                           Map<String, SimpleBrokerMessageHandler> brokerHandlers) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.broadcastSessionManager = broadcastSessionManager;
        this.brokerHandlers = brokerHandlers;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
        config.setCacheLimit(20000);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void replaceSynchronizedRegistry() {
        brokerHandlers.values()
                .forEach(handler -> handler.setSubscriptionRegistry(new LockFreeSubscriptionRegistry()));
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setSendTimeLimit(30000);
        registration.setSendBufferSizeLimit(2048 * 1024);
        registration.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                broadcastSessionManager.register(session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                broadcastSessionManager.unregister(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        });
    }
}
