package com.healthcare.claimsystem.security;

import com.healthcare.claimsystem.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final AppProperties appProperties;
    private final SecretKey signingKey;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.signingKey = Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair generateTokens(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plus(appProperties.getJwt().getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);
        Instant refreshExpiry = now.plus(appProperties.getJwt().getRefreshTokenExpirationDays(), ChronoUnit.DAYS);
        String accessToken = buildToken(principal, TYPE_ACCESS, now, accessExpiry);
        String refreshToken = buildToken(principal, TYPE_REFRESH, now, refreshExpiry);
        long expiresIn = appProperties.getJwt().getAccessTokenExpirationMinutes() * 60;
        return new TokenPair(accessToken, refreshToken, expiresIn);
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parseClaims(token).get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get("type", String.class));
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(appProperties.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public void validateRefreshToken(String token) {
        if (!isRefreshToken(token)) {
            throw new JwtException("Invalid token type");
        }
    }

    private String buildToken(UserPrincipal principal, String type, Instant issuedAt, Instant expiration) {
        return Jwts.builder()
                .issuer(appProperties.getJwt().getIssuer())
                .subject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("role", principal.getRole().name())
                .claim("type", type)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    }
}
