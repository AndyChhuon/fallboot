package com.andy.fallboot.shared.config.interceptor;

import com.andy.fallboot.pixel.component.BroadcastSessionManager;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final BroadcastSessionManager broadcastSessionManager;

    public StompAuthChannelInterceptor(JwtDecoder jwtDecoder, BroadcastSessionManager broadcastSessionManager) {
        this.jwtDecoder = jwtDecoder;
        this.broadcastSessionManager = broadcastSessionManager;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                Jwt jwt = jwtDecoder.decode(authHeader.substring(7));
                accessor.setUser(new StompPrincipal(jwt.getClaimAsString("sub")));
            } else {
                throw new MessagingException("Missing or invalid Authorization header");
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            broadcastSessionManager.markReady(accessor.getSessionId());
        }

        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            broadcastSessionManager.unregister(accessor.getSessionId());
        }

        return message;
    }
}
