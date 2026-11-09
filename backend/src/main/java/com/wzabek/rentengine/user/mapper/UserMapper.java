package com.wzabek.rentengine.user.mapper;

import com.wzabek.rentengine.user.dto.UserResponse;
import com.wzabek.rentengine.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}
