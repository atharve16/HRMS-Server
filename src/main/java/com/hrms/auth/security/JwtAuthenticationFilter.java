package com.hrms.auth.security;

import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.entity.UserStatus;
import com.hrms.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(authorization.substring(7));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String rawToken) {
        try {
            Jwt jwt = jwtService.decode(rawToken);
            String userId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String roleClaim = jwt.getClaimAsString("role");
            UserRole claimedRole = UserRole.valueOf(roleClaim);
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE
                    || user.getRole() != claimedRole || !user.getNormalizedEmail().equals(email)) {
                return;
            }
            AuthPrincipal principal = new AuthPrincipal(user.getId(), user.getNormalizedEmail(), user.getRole());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
