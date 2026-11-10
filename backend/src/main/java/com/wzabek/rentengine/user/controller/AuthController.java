package com.wzabek.rentengine.user.controller;

import com.wzabek.rentengine.user.dto.AuthResponse;
import com.wzabek.rentengine.user.dto.LoginRequest;
import com.wzabek.rentengine.user.dto.RegisterRequest;
import com.wzabek.rentengine.user.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Auth")
@SecurityRequirements // logowanie i rejestracja bez tokena
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
