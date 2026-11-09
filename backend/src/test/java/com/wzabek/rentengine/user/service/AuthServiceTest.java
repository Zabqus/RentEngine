package com.wzabek.rentengine.user.service;

import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.user.dto.AuthResponse;
import com.wzabek.rentengine.user.dto.LoginRequest;
import com.wzabek.rentengine.user.dto.RegisterRequest;
import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import com.wzabek.rentengine.user.mapper.UserMapper;
import com.wzabek.rentengine.user.repository.UserRepository;
import com.wzabek.rentengine.user.security.JwtProperties;
import com.wzabek.rentengine.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    // Niski koszt BCrypta, żeby testy były szybkie
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService = new JwtService(
            new JwtProperties("test-secret-that-is-at-least-32-bytes-long!", Duration.ofHours(24)), Clock.systemUTC());

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, jwtService, new UserMapper());
    }

    @Test
    void registerCreatesClientWithHashedPasswordAndNormalizedEmail() {
        when(userRepository.existsByEmail("jan@test.pl")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), 7L));

        AuthResponse response = service.register(new RegisterRequest("  Jan@Test.PL ", "Secret123!", " Jan ", "Kowalski"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("jan@test.pl");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.CLIENT);
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("Secret123!");
        assertThat(passwordEncoder.matches("Secret123!", saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.user().firstName()).isEqualTo("Jan");
        assertThat(jwtService.parse(response.accessToken()).getSubject()).isEqualTo("7");
    }

    @Test
    void registerWithTakenEmailIsRejected() {
        when(userRepository.existsByEmail("jan@test.pl")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("jan@test.pl", "Secret123!", "Jan", "K")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("EMAIL_TAKEN");
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithCorrectPasswordReturnsToken() {
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(Optional.of(storedUser()));

        AuthResponse response = service.login(new LoginRequest("JAN@test.pl", "Secret123!"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwtService.parse(response.accessToken()).get("role", String.class)).isEqualTo("CLIENT");
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        when(userRepository.findByEmail("jan@test.pl")).thenReturn(Optional.of(storedUser()));

        assertThatThrownBy(() -> service.login(new LoginRequest("jan@test.pl", "wrong")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginWithUnknownEmailGivesSameError() {
        when(userRepository.findByEmail("nobody@test.pl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@test.pl", "Secret123!")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    private User storedUser() {
        return withId(new User("jan@test.pl", passwordEncoder.encode("Secret123!"), "Jan", "Kowalski", Role.CLIENT), 1L);
    }

    private static User withId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
