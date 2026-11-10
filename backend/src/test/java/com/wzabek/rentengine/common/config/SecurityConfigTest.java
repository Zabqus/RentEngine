package com.wzabek.rentengine.common.config;

import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import com.wzabek.rentengine.user.security.AuthenticatedUser;
import com.wzabek.rentengine.user.security.JwtProperties;
import com.wzabek.rentengine.user.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, SecurityConfigTest.TestController.class, SecurityConfigTest.JwtTestConfig.class})
class SecurityConfigTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void publicCatalogGetWorksWithoutToken() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
    }

    @Test
    void publicEndpointIgnoresInvalidToken() throws Exception {
        mvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401ProblemDetail() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired token"));
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        JwtService yesterday = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)),
                Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC));

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + yesterday.generateToken(user())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired token"));
    }

    @Test
    void validTokenAuthenticatesUser() throws Exception {
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    private static User user() {
        User user = new User("jan@test.pl", "hash", "Jan", "Kowalski", Role.CLIENT);
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }

    @RestController
    static class TestController {

        @GetMapping("/api/categories")
        String publicEndpoint() {
            return "ok";
        }

        @GetMapping("/api/me")
        AuthenticatedUser me(@AuthenticationPrincipal AuthenticatedUser user) {
            return user;
        }
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtService jwtService() {
            return new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)), Clock.systemUTC());
        }
    }
}
