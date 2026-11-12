package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.auth.RegisterRequest;
import com.healthcare.claimsystem.dto.user.UserResponse;
import com.healthcare.claimsystem.dto.user.UserStatusRequest;
import com.healthcare.claimsystem.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse register(RegisterRequest request);

    Page<UserResponse> list(Role role, Boolean enabled, String search, Pageable pageable);

    UserResponse updateStatus(Long id, UserStatusRequest request);
}
