package com.wzabek.rentengine.user.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Nie jest @Component, żeby Boot nie zarejestrował go drugi raz poza łańcuchem Spring Security
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String INVALID_TOKEN_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".invalid";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                AuthenticatedUser user = jwtService.parseUser(header.substring(BEARER_PREFIX.length()));
                var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (JwtException | IllegalArgumentException ex) {
                // Zły token: request leci dalej jako anonimowy, chroniony endpoint odda 401
                request.setAttribute(INVALID_TOKEN_ATTRIBUTE, true);
            }
        }
        chain.doFilter(request, response);
    }
}
