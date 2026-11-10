package com.wzabek.rentengine.user.security;

import com.wzabek.rentengine.user.entity.Role;

// Zalogowany użytkownik odczytany z tokena; dostępny w kontrolerach przez @AuthenticationPrincipal
public record AuthenticatedUser(Long id, String email, Role role) {
}
