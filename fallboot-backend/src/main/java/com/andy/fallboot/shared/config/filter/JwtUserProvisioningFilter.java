package com.andy.fallboot.shared.config.filter;

import com.andy.fallboot.shared.userEntities.UserDTO;
import com.andy.fallboot.user.UserProvisioningProducer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtUserProvisioningFilter extends OncePerRequestFilter {
    private final UserProvisioningProducer userProvisioningProducer;
    private final Cache<String, Boolean> provisionedUsers = Caffeine.newBuilder()
            .maximumSize(1_000_000)
            .build();

    public JwtUserProvisioningFilter(UserProvisioningProducer userProvisioningProducer) {
        this.userProvisioningProducer = userProvisioningProducer;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()){
            Object principal = authentication.getPrincipal();

            if (principal instanceof Jwt jwt){
                String cognitoId = jwt.getClaimAsString("sub");
                String email = jwt.getClaimAsString("email");
                String name = jwt.getClaimAsString("given_name");

                request.setAttribute("cognitoId", cognitoId);

                provisionedUsers.get(cognitoId, _ -> {
                    userProvisioningProducer.sendUserProvisioning(new UserDTO(cognitoId, email, name));
                    return true;
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/public") || request.getRequestURI().startsWith("/actuator");
    }
}
