package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.auth.LoginRequest;
import com.healthcare.claimsystem.dto.auth.LoginResponse;
import com.healthcare.claimsystem.dto.auth.RefreshTokenRequest;
import com.healthcare.claimsystem.dto.auth.TokenResponse;
import com.healthcare.claimsystem.dto.user.UserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    UserResponse currentUser();
}
