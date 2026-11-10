package com.wzabek.rentengine.user.service;

import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import com.wzabek.rentengine.user.dto.UserCreateRequest;
import com.wzabek.rentengine.user.dto.UserResponse;
import com.wzabek.rentengine.user.dto.UserUpdateRequest;
import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import com.wzabek.rentengine.user.mapper.UserMapper;
import com.wzabek.rentengine.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long ADMIN_ID = 4L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RentalAgreementRepository agreementRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, agreementRepository, passwordEncoder, new UserMapper());
    }

    @Test
    void adminCanCreateEmployee() {
        when(userRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), 9L));

        UserResponse created = service.create(
                new UserCreateRequest("Ewa@Firma.PL", "Secret123!", "Ewa", "Nowak", Role.EMPLOYEE));

        assertThat(created.role()).isEqualTo(Role.EMPLOYEE);
        assertThat(created.email()).isEqualTo("ewa@firma.pl");
    }

    @Test
    void createWithTakenEmailIsRejected() {
        when(userRepository.existsByEmail("ewa@firma.pl")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new UserCreateRequest("ewa@firma.pl", "Secret123!", "Ewa", "Nowak", Role.EMPLOYEE)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void updateWithoutPasswordKeepsOldHash() {
        User user = user(1L, Role.CLIENT);
        String oldHash = user.getPasswordHash();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.update(1L, new UserUpdateRequest("jan@test.pl", null, "Jan", "Kowalski", Role.EMPLOYEE), ADMIN_ID);

        assertThat(user.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(user.getPasswordHash()).isEqualTo(oldHash);
    }

    @Test
    void updateWithPasswordSetsNewHash() {
        User user = user(1L, Role.CLIENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.update(1L, new UserUpdateRequest("jan@test.pl", "NewSecret1!", "Jan", "Kowalski", Role.CLIENT),
                ADMIN_ID);

        assertThat(passwordEncoder.matches("NewSecret1!", user.getPasswordHash())).isTrue();
    }

    @Test
    void adminCannotChangeOwnRole() {
        User admin = user(ADMIN_ID, Role.ADMIN);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.update(ADMIN_ID,
                new UserUpdateRequest("admin@test.pl", null, "Ad", "Min", Role.CLIENT), ADMIN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("CANNOT_CHANGE_OWN_ROLE");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void adminCannotDeleteSelf() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, Role.ADMIN)));

        assertThatThrownBy(() -> service.delete(ADMIN_ID, ADMIN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("CANNOT_DELETE_SELF");
        verify(userRepository, never()).delete(any());
    }

    @Test
    void userWithAgreementsCannotBeDeleted() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.CLIENT)));
        when(agreementRepository.existsByClientId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L, ADMIN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("USER_HAS_AGREEMENTS");
        verify(userRepository, never()).delete(any());
    }

    private User user(Long id, Role role) {
        return withId(new User("jan@test.pl", passwordEncoder.encode("Secret123!"), "Jan", "Kowalski", role), id);
    }

    private static User withId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
