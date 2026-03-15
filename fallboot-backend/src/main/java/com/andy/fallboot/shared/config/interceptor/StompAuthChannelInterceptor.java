package com.andy.fallboot.shared.config.interceptor;

import com.andy.fallboot.shared.userEntities.UserDTO;
import com.andy.fallboot.user.UserService;
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
    private final UserService userService;

    public StompAuthChannelInterceptor(JwtDecoder jwtDecoder, UserService userService) {
        this.jwtDecoder = jwtDecoder;
        this.userService = userService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Jwt jwt = jwtDecoder.decode(token);

                String cognitoId = jwt.getClaimAsString("sub");
                String email = jwt.getClaimAsString("email");
                String name = jwt.getClaimAsString("given_name");

                UserDTO user = userService.findOrCreateUser(cognitoId, email, name);

                accessor.setUser(new StompPrincipal(user.id().toString()));
            } else {
                throw new MessagingException("Missing or invalid Authorization header");
            }
        }

        return message;
    }
}