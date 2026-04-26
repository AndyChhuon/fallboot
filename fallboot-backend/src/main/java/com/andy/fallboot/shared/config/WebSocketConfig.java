package com.andy.fallboot.shared.config;

import com.andy.fallboot.pixel.component.PixelWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {
    private final PixelWebSocketHandler pixelWebSocketHandler;

    public WebSocketConfig(PixelWebSocketHandler pixelWebSocketHandler) {
        this.pixelWebSocketHandler = pixelWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pixelWebSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }
}
