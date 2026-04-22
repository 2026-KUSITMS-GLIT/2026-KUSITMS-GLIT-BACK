package com.groute.groute_server.auth.service.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.jwt.JwtTokenProvider;
import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.auth.service.TokenDeliveryService;
import com.groute.groute_server.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 로그인 성공 시 JWT를 발급하고 클라이언트에 전달.
 *
 * <p>access/refresh 발급 → refresh를 Redis에 저장 → {@link TokenDeliveryService}가 설정(쿠키/본문)에 맞게 응답 구성. 인가
 * 코드 교환 중 잠시 사용된 세션은 더 이상 필요 없으므로 명시적으로 invalidate하여 서버 상태를 JWT-only로 복귀시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenDeliveryService tokenDeliveryService;
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

        TokenResponse body = tokenDeliveryService.deliver(response, accessToken, refreshToken);

        writeJson(response, body);
        invalidateSession(request);
        log.info("OAuth2 로그인 성공: userId={}, provider={}", userId, principal.getProvider());
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
