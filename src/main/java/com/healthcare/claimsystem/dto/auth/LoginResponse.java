package com.healthcare.claimsystem.dto.auth;

import com.healthcare.claimsystem.dto.user.UserResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
