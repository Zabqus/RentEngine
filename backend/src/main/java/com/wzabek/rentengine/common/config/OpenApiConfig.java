package com.wzabek.rentengine.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Swagger UI: /swagger-ui.html. Token z /api/auth/login wklejamy w "Authorize".
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "RentEngine API", version = "v1",
                description = "Car and machine rental: catalog, reservations, protocols, settlements"),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER))
@SecurityScheme(name = OpenApiConfig.BEARER, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";
}
