package com.groute.groute_server.auth.service;

import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.groute.groute_server.auth.config.AuthProperties;
import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.jwt.JwtProperties;

import lombok.RequiredArgsConstructor;

/**
 * 발급된 access/refresh 토큰을 설정에 따라 쿠키 또는 본문으로 전달하고, 클라이언트에 돌아갈 {@link TokenResponse}를 구성.
 *
 * <p>OAuth2 로그인 성공과 재발급 엔드포인트가 동일한 전달 규칙을 공유하기 위한 공통화 지점. 쿠키 모드에서는 refresh를 {@code HttpOnly;
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
            response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken).toString());
            return new TokenResponse(accessToken, null);
        }
        return new TokenResponse(accessToken, refreshToken);
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE_STRICT)
                .path(COOKIE_PATH)
                .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpiration()))
                .build();
    }
}
