package com.healthcare.claimsystem.security;

import com.healthcare.claimsystem.config.AppProperties;
import com.healthcare.claimsystem.entity.enums.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("test-secret-key-for-healthcare-insurance-system-2026");
        properties.getJwt().setIssuer("healthcare-test");
        properties.getJwt().setAccessTokenExpirationMinutes(30);
        properties.getJwt().setRefreshTokenExpirationDays(2);
        jwtService = new JwtService(properties);
        principal = new UserPrincipal(1L, "test-user", "encoded", "Test User", Role.INSURANCE_OFFICER, true);
    }

    @Test
    void generatesAccessAndRefreshTokens() {
        JwtService.TokenPair tokens = jwtService.generateTokens(principal);

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.expiresIn()).isEqualTo(1800);
        assertThat(jwtService.isAccessToken(tokens.accessToken())).isTrue();
        assertThat(jwtService.isRefreshToken(tokens.accessToken())).isFalse();
        assertThat(jwtService.isRefreshToken(tokens.refreshToken())).isTrue();
        assertThat(jwtService.extractUsername(tokens.accessToken())).isEqualTo("test-user");
    }

    @Test
    void rejectsRefreshTokenAsAccessToken() {
        JwtService.TokenPair tokens = jwtService.generateTokens(principal);

        assertThat(jwtService.isAccessToken(tokens.refreshToken())).isFalse();
    }

    @Test
    void rejectsInvalidToken() {
        assertThatThrownBy(() -> jwtService.parseClaims("not-a-token"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsAccessTokenForRefreshFlow() {
        JwtService.TokenPair tokens = jwtService.generateTokens(principal);

        assertThatThrownBy(() -> jwtService.validateRefreshToken(tokens.accessToken()))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid token type");
    }
}
