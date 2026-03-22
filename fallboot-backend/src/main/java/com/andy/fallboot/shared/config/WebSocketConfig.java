package com.andy.fallboot.shared.config;

import com.andy.fallboot.shared.config.interceptor.StompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import reactor.netty.tcp.TcpClient;

@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    private final String relayHost;
    private final int relayPort;
    private final String relayUsername;
    private final String relayPassword;
    private final boolean relaySsl;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
                           @Value("${spring.rabbitmq.host}") String relayHost,
                           @Value("${spring.rabbitmq.port}") int relayPort,
                           @Value("${spring.rabbitmq.username}") String relayUsername,
                           @Value("${spring.rabbitmq.password}") String relayPassword,
                           @Value("${spring.rabbitmq.ssl:false}") boolean relaySsl) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.relayUsername = relayUsername;
        this.relayPassword = relayPassword;
        this.relaySsl = relaySsl;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        var relay = config.enableStompBrokerRelay("/topic")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setVirtualHost("/")
                .setClientLogin(relayUsername)
                .setClientPasscode(relayPassword)
                .setSystemLogin(relayUsername)
                .setSystemPasscode(relayPassword);

        if (relaySsl) {
            TcpClient tcpClient = TcpClient.create()
                    .host(relayHost)
                    .port(relayPort)
                    .secure();
            relay.setTcpClient(new ReactorNettyTcpClient<>(tcpClient,
                    new StompReactorNettyCodec()));
        }

        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
        registration.taskExecutor().corePoolSize(50).maxPoolSize(200);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor().corePoolSize(50).maxPoolSize(200);
    }
}
