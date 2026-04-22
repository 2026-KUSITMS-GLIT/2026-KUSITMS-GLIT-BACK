package com.groute.groute_server.auth.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.response.ErrorResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증은 되었으나 권한이 부족한 요청에 대한 핸들러.
 *
 * <p>현재 역할 체크가 없어 실제 발동 경로는 드물지만, 미래 {@code @PreAuthorize} 도입 대비로 응답 포맷을 {@link ErrorResponse} JSON
 * 403으로 통일해둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        log.debug(
                "권한 거부: path={}, reason={}",
                request.getRequestURI(),
                accessDeniedException.getMessage());
        response.setStatus(ErrorCode.FORBIDDEN.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(ErrorCode.FORBIDDEN));
    }
}
