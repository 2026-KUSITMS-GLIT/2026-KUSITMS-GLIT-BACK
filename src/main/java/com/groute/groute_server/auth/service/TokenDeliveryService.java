package com.groute.groute_server.auth.service;

import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.groute.groute_server.auth.config.AuthProperties;
import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.common.jwt.JwtProperties;

import lombok.RequiredArgsConstructor;

/**
 * 발급된 access/refresh 토큰을 설정에 따라 쿠키 또는 본문으로 전달하고, 로그아웃 시 refresh 쿠키 삭제까지 담당.
 *
 * <p>OAuth2 로그인 성공·재발급·로그아웃이 동일한 쿠키 전달 규칙을 공유하기 위한 공통화 지점. 쿠키 모드에서는 refresh를 {@code HttpOnly;
 * Secure; SameSite=Strict} 쿠키에 싣고 본문에서는 생략, 본문 모드에서는 둘 다 본문에 담는다.
 */
@Component
@RequiredArgsConstructor
public class TokenDeliveryService {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE_STRICT = "Strict";

    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;

    public TokenResponse deliver(
            HttpServletResponse response, String accessToken, String refreshToken) {
        if (authProperties.refreshToken().cookieEnabled()) {
            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    buildRefreshCookie(
                                    refreshToken,
                                    Duration.ofMillis(
                                            jwtProperties.refreshTokenExpiration())) // 쿠키 만료시간 설정
                            .toString());
            return new TokenResponse(accessToken, null);
        }
        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * 쿠키 프로필에서 refresh 쿠키를 만료시켜 브라우저 측에서 삭제되게 한다.
     *
     * <p>{@link #deliver}가 심은 쿠키와 동일한 속성(name·path·HttpOnly·Secure·SameSite)으로 maxAge만 0으로 내려야
     * 브라우저가 기존 쿠키와 매칭해 덮어쓰며 제거한다. 속성이 하나라도 다르면 별개 쿠키로 인식되어 기존 쿠키가 그대로 남는다. 비쿠키 프로필에서는 no-op.
     */
    public void clear(HttpServletResponse response) {
        if (!authProperties.refreshToken().cookieEnabled()) {
            return;
        }
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildRefreshCookie("", Duration.ZERO).toString()); // 쿠기 즉시 무효화
    }

    private ResponseCookie buildRefreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE_STRICT)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
