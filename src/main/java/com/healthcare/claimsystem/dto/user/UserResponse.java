package com.healthcare.claimsystem.dto.user;

import com.healthcare.claimsystem.entity.enums.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
