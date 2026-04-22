package com.groute.groute_server.auth.service.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.response.ErrorResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 로그인 실패(사용자 취소·scope 거부·provider 오류·정규화 예외 등)를 JSON 401로 변환.
 *
 * <p>기본 스프링 동작은 {@code /login?error}로 302 리다이렉트라 API 클라이언트 입장에서는 HTML이 내려와 혼란스러워진다. 이 핸들러는 일관된
 * {@link ErrorResponse} 포맷을 유지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        log.warn("OAuth2 로그인 실패: {}", exception.getMessage());
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                ErrorResponse.of(ErrorCode.UNAUTHORIZED, exception.getMessage()));
    }
}
