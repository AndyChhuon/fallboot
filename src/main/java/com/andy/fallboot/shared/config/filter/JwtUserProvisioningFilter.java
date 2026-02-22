package com.andy.fallboot.shared.config.filter;

import com.andy.fallboot.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtUserProvisioningFilter extends OncePerRequestFilter {
    private final UserService userService;

    @Autowired
    public JwtUserProvisioningFilter(UserService userService){
        this.userService = userService;
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

                userService.findOrCreateUser(cognitoId, email, name);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/public");
    }
}
