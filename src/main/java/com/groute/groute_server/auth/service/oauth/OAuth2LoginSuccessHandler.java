package com.groute.groute_server.auth.service.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groute.groute_server.auth.config.AuthProperties;
import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.jwt.JwtProperties;
import com.groute.groute_server.auth.jwt.JwtTokenProvider;
import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 로그인 성공 시 JWT를 발급하고 클라이언트에 전달.
 *
 * <p>access/refresh 발급 → refresh를 Redis에 저장 → 응답 전송의 세 단계로 흐른다. refresh 전달 방식은 {@link
 * AuthProperties#refreshToken()} 설정에 따라 분기:
 *
 * <ul>
 *   <li>쿠키 모드(prod): {@code HttpOnly; Secure; SameSite=Strict} 쿠키로 전달, 본문에는 access만
 *   <li>본문 모드(local/stg): access와 refresh 모두 JSON 본문으로 전달 (로컬 HTTP 개발 편의)
 * </ul>
 *
 * <p>OAuth2 인가 코드 교환 중 잠시 사용된 세션은 더 이상 필요 없으므로 명시적으로 invalidate하여 서버 상태를 JWT-only로 복귀시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE_STRICT = "Strict";

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        PrincipalUser principal = (PrincipalUser) authentication.getPrincipal();
        Long userId = principal.getUserId();

        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        refreshTokenRepository.save(userId, refreshToken);

        TokenResponse body;
        if (authProperties.refreshToken().cookieEnabled()) {
            response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken).toString());
            body = new TokenResponse(accessToken, null);
        } else {
            body = new TokenResponse(accessToken, refreshToken);
        }

        writeJson(response, body);
        invalidateSession(request);
        log.info("OAuth2 로그인 성공: userId={}, provider={}", userId, principal.getProvider());
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

    private void writeJson(HttpServletResponse response, TokenResponse body) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.ok(body));
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
