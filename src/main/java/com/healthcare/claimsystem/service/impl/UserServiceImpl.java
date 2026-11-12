package com.healthcare.claimsystem.service.impl;

import com.healthcare.claimsystem.dto.auth.RegisterRequest;
import com.healthcare.claimsystem.dto.user.UserResponse;
import com.healthcare.claimsystem.dto.user.UserStatusRequest;
import com.healthcare.claimsystem.entity.User;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;
import com.healthcare.claimsystem.entity.enums.Role;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.DuplicateResourceException;
import com.healthcare.claimsystem.exception.ResourceNotFoundException;
import com.healthcare.claimsystem.mapper.UserMapper;
import com.healthcare.claimsystem.repository.UserRepository;
import com.healthcare.claimsystem.service.AuditService;
import com.healthcare.claimsystem.service.UserService;
import com.healthcare.claimsystem.specification.UserSpecifications;
import com.healthcare.claimsystem.util.PageableUtils;
import com.healthcare.claimsystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "username", "email", "fullName", "role");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setEnabled(true);
        User saved = userRepository.save(user);
        auditService.recordEntity(
                AuditEntityType.USER,
                saved.getId(),
                AuditAction.USER_REGISTERED,
                "User " + saved.getUsername() + " registered with role " + saved.getRole()
        );
        return UserMapper.toResponse(saved);
    }

    @Override
    public Page<UserResponse> list(Role role, Boolean enabled, String search, Pageable pageable) {
        Pageable sanitized = PageableUtils.sanitize(pageable, SORT_FIELDS, "createdAt");
        return userRepository.findAll(UserSpecifications.withFilters(role, enabled, search), sanitized)
                .map(UserMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, UserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        boolean targetEnabled = request.enabled();
        if (!targetEnabled) {
            String currentUsername = SecurityUtils.currentUsername();
            if (user.getUsername().equals(currentUsername)) {
                throw new BusinessRuleException("You cannot disable your own account");
            }
        }
        user.setEnabled(targetEnabled);
        User saved = userRepository.save(user);
        auditService.recordEntity(
                AuditEntityType.USER,
                saved.getId(),
                AuditAction.USER_STATUS_CHANGED,
                "User " + saved.getUsername() + " enabled set to " + targetEnabled
        );
        return UserMapper.toResponse(saved);
    }
}
