package com.wzabek.rentengine.user.dto;

import com.wzabek.rentengine.user.entity.Role;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role
) {
}
