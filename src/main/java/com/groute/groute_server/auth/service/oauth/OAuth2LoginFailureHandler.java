package com.groute.groute_server.auth.service.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 로그인 실패(사용자 취소·scope 거부·provider 오류·정규화 예외 등)를 프론트 콜백 URL로 redirect하여 일관된 UX를 제공.
 *
 * <p>성공 흐름과 동일한 콜백 URL을 사용하되, query에 {@code ?error=<ErrorCode>}를 붙여 프론트가 분기 처리할 수 있게 한다. 콜백 URL은
 * {@link OAuthCallbackUrlResolver}가 {@code oauth_env} 쿠키 기반으로 결정하며 사용 후 즉시 만료한다.
 *
 * <p>{@code CustomOAuth2UserService}가 {@link BusinessException}을 {@link
 * org.springframework.security.oauth2.core.OAuth2AuthenticationException}의 cause로 감싸 던지므로, 원인 체인에서
 * {@link BusinessException}을 찾아 해당 {@link ErrorCode}를 query에 반영한다. 없으면 기본 {@link
 * ErrorCode#UNAUTHORIZED}.
 *
 * <p><b>응답 본문 미사용</b>: 사용자 메시지는 query에 노출하지 않는다(인코딩·정보 노출 부담). code만 전달하고 상세 사유는 서버 로그에 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuthCallbackUrlResolver oAuthCallbackUrlResolver;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        ErrorCode errorCode = resolveErrorCode(exception);
        String callbackUrl = oAuthCallbackUrlResolver.resolveAndExpire(request, response);
        String redirectUrl = buildErrorRedirectUrl(callbackUrl, errorCode.getCode());

        log.warn(
                "OAuth2 로그인 실패: code={}, message={}, callback={}",
                errorCode.getCode(),
                exception.getMessage(),
                callbackUrl);
        response.sendRedirect(redirectUrl);
    }

    private static String buildErrorRedirectUrl(String callbackUrl, String errorCode) {
        // 콜백 URL이 이미 query를 포함하면 `&`로 이어 붙여 URL이 깨지지 않게 한다.
        String separator = callbackUrl.contains("?") ? "&" : "?";
        return callbackUrl
                + separator
                + "error="
                + URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
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
