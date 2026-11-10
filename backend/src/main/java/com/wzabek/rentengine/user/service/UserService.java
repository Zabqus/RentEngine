package com.wzabek.rentengine.user.service;

import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.common.exception.NotFoundException;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import com.wzabek.rentengine.user.dto.UserCreateRequest;
import com.wzabek.rentengine.user.dto.UserResponse;
import com.wzabek.rentengine.user.dto.UserUpdateRequest;
import com.wzabek.rentengine.user.entity.User;
import com.wzabek.rentengine.user.mapper.UserMapper;
import com.wzabek.rentengine.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Zarządzanie kontami przez admina (zakładanie pracowników, zmiana ról, usuwanie)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RentalAgreementRepository agreementRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public UserResponse findById(Long id) {
        return mapper.toResponse(getUser(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw emailTaken(email);
        }
        User user = new User(email, passwordEncoder.encode(request.password()),
                request.firstName().trim(), request.lastName().trim(), request.role());
        return mapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request, Long currentUserId) {
        User user = getUser(id);
        // Admin nie może sam sobie odebrać roli, bo mógłby zostać system bez admina
        if (id.equals(currentUserId) && request.role() != user.getRole()) {
            throw new BusinessRuleException("CANNOT_CHANGE_OWN_ROLE", "You cannot change your own role");
        }
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw emailTaken(email);
        }
        user.setEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setRole(request.role());
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return mapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        User user = getUser(id);
        if (id.equals(currentUserId)) {
            throw new BusinessRuleException("CANNOT_DELETE_SELF", "You cannot delete your own account");
        }
        if (agreementRepository.existsByClientId(id)) {
            throw new BusinessRuleException("USER_HAS_AGREEMENTS",
                    "User with id %d has rental agreements and cannot be deleted".formatted(id));
        }
        userRepository.delete(user);
    }

    private User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    private static BusinessRuleException emailTaken(String email) {
        return new BusinessRuleException("EMAIL_TAKEN", "Email '%s' is already registered".formatted(email));
    }
}
