package com.wzabek.rentengine.user.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration expiration
) {
}
