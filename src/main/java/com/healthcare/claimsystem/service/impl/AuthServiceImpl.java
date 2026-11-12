package com.healthcare.claimsystem.service.impl;

import com.healthcare.claimsystem.dto.auth.LoginRequest;
import com.healthcare.claimsystem.dto.auth.LoginResponse;
import com.healthcare.claimsystem.dto.auth.RefreshTokenRequest;
import com.healthcare.claimsystem.dto.auth.TokenResponse;
import com.healthcare.claimsystem.dto.user.UserResponse;
import com.healthcare.claimsystem.entity.User;
import com.healthcare.claimsystem.exception.ResourceNotFoundException;
import com.healthcare.claimsystem.mapper.UserMapper;
import com.healthcare.claimsystem.repository.UserRepository;
import com.healthcare.claimsystem.security.JwtService;
import com.healthcare.claimsystem.security.UserPrincipal;
import com.healthcare.claimsystem.service.AuthService;
import com.healthcare.claimsystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().trim(), request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", principal.getUsername()));
        JwtService.TokenPair tokens = jwtService.generateTokens(principal);
        return new LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                tokens.expiresIn(),
                UserMapper.toResponse(user)
        );
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        jwtService.validateRefreshToken(request.refreshToken());
        String username = jwtService.extractUsername(request.refreshToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }
        JwtService.TokenPair tokens = jwtService.generateTokens(UserPrincipal.from(user));
        return new TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                tokens.expiresIn()
        );
    }

    @Override
    public UserResponse currentUser() {
        String username = SecurityUtils.currentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return UserMapper.toResponse(user);
    }
}
