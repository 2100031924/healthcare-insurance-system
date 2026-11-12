package com.healthcare.claimsystem.config;

import com.healthcare.claimsystem.entity.User;
import com.healthcare.claimsystem.entity.enums.Role;
import com.healthcare.claimsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(1)
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.BootstrapAdmin admin = appProperties.getBootstrapAdmin();
        if (userRepository.existsByUsername(admin.getUsername())) {
            return;
        }

        User user = new User();
        user.setUsername(admin.getUsername());
        user.setEmail(admin.getEmail());
        user.setPassword(passwordEncoder.encode(admin.getPassword()));
        user.setFullName(admin.getFullName());
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Bootstrap administrator {} created. Change the default password before deployment.", admin.getUsername());
    }
}
