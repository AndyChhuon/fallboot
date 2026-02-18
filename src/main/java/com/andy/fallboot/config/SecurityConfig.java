package com.andy.fallboot.config;

import com.andy.fallboot.config.filter.JwtUserProvisioningFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtUserProvisioningFilter jwtUserProvisioningFilter;


    public SecurityConfig(JwtUserProvisioningFilter jwtUserProvisioningFilter) {
        this.jwtUserProvisioningFilter = jwtUserProvisioningFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/api/**").authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                }))
                .addFilterAfter(jwtUserProvisioningFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
