package com.groute.groute_server.common.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT access/refresh 토큰 발급·파싱·검증.
 *
 * <p>subject에 userId, custom claim {@code type}에 {@link TokenType}을 저장한다. 검증은 예외를 던지지 않는 {@link
 * #validate(String)}과, 통과 후 claim을 꺼내는 {@link #getUserId(String)} / {@link #getTokenType(String)}을
 * 조합해 사용한다. Security 필터/재발급 API에서의 401 분기는 {@link JwtValidationResult}로 일원화한다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TOKEN_TYPE = "type";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId) {
        return buildToken(userId, TokenType.ACCESS, properties.accessTokenExpiration());
    }

    public String createRefreshToken(Long userId) {
        return buildToken(userId, TokenType.REFRESH, properties.refreshTokenExpiration());
    }

    /** 서명·형식·만료를 모두 검사. 예외를 던지지 않고 {@link JwtValidationResult}로 반환한다. */
    public JwtValidationResult validate(String token) {
        try {
            parseClaims(token);
            return JwtValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            return JwtValidationResult.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 검증 실패: {}", e.getMessage());
            return JwtValidationResult.INVALID;
        }
    }

    /** 유효한 토큰의 subject(userId) 추출. 만료/위조 토큰이면 {@link JwtException} 발생. */
    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /** 유효한 토큰의 {@code type} claim 추출. */
    public TokenType getTokenType(String token) {
        String type = parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
        return TokenType.valueOf(type);
    }

    private String buildToken(Long userId, TokenType type, long expirationMs) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, type.name())
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
