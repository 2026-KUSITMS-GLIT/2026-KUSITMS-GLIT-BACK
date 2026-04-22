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
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.response.ErrorResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 로그인 실패(사용자 취소·scope 거부·provider 오류·정규화 예외 등)를 일관된 JSON 에러 응답으로 변환.
 *
 * <p>기본 스프링 동작은 {@code /login?error}로 302 리다이렉트라 API 클라이언트 입장에서는 HTML이 내려와 혼란스러워진다.
 *
 * <p>{@link CustomOAuth2UserService}가 {@link BusinessException}을 {@link
 * OAuth2AuthenticationException}의 cause로 감싸 던지기 때문에, 원인 체인에서 {@link BusinessException}을 찾아 해당
 * {@link ErrorCode}를 응답으로 반영한다. 없으면 기본 {@link ErrorCode#UNAUTHORIZED}.
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
        ErrorCode errorCode = resolveErrorCode(exception);
        log.warn("OAuth2 로그인 실패: code={}, message={}", errorCode.getCode(), exception.getMessage());
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(), ErrorResponse.of(errorCode, exception.getMessage()));
    }

    private ErrorCode resolveErrorCode(AuthenticationException exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof BusinessException be) {
                return be.getErrorCode();
            }
            cause = cause.getCause();
        }
        return ErrorCode.UNAUTHORIZED;
    }
}
