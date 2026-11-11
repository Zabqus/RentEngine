package com.wzabek.rentengine.notification.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.mail")
public record MailProperties(
        @NotBlank @Email String from,
        boolean enabled
) {
}
