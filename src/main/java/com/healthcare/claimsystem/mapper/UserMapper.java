package com.healthcare.claimsystem.mapper;

import com.healthcare.claimsystem.dto.user.UserResponse;
import com.healthcare.claimsystem.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
