package com.wzabek.rentengine.user.service;

import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.user.dto.AuthResponse;
import com.wzabek.rentengine.user.dto.LoginRequest;
import com.wzabek.rentengine.user.dto.RegisterRequest;
import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import com.wzabek.rentengine.user.mapper.UserMapper;
import com.wzabek.rentengine.user.repository.UserRepository;
import com.wzabek.rentengine.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper mapper;

    // Rejestracja zawsze tworzy klienta; konta pracowników i adminów zakłada admin
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("EMAIL_TAKEN", "Email '%s' is already registered".formatted(email));
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()),
                request.firstName().trim(), request.lastName().trim(), Role.CLIENT));
        return authResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                // Ten sam komunikat dla złego emaila i hasła, żeby nie zdradzać, które konta istnieją
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return authResponse(user);
    }

    private AuthResponse authResponse(User user) {
        return AuthResponse.bearer(jwtService.generateToken(user), jwtService.expirationSeconds(),
                mapper.toResponse(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
