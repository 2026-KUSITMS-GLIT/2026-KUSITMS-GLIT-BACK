package com.groute.groute_server.auth.service.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.groute.groute_server.auth.config.AuthProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 round-trip 후 프론트 콜백 URL을 결정하는 공용 컴포넌트.
 *
 * <p>{@link OAuth2EnvAwareAuthorizationRequestResolver}가 OAuth2 시작 시 발행한 {@code oauth_env} 쿠키를 읽어
 * {@link AuthProperties#callback()} 화이트리스트와 매칭한다. 매칭 성공이면 그 URL을, 쿠키가 없거나 등록되지 않은 env이면 default로
 * fallback. 사용 후에는 즉시 만료 처리(Max-Age=0)해 round-trip 흔적을 지운다.
 *
 * <p>SuccessHandler·FailureHandler 두 곳에서 동일하게 호출하여 콜백 URL 정책을 단일 진실로 유지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthCallbackUrlResolver {

    private final AuthProperties authProperties;

    /**
     * 콜백 URL을 해석하고 round-trip 쿠키를 만료시킨다.
     *
     * @param request 현재 요청 (쿠키 + secure 플래그 추출용)
     * @param response 만료 쿠키 set-cookie 헤더를 추가할 응답
     * @return 매칭된 또는 default 콜백 URL
     */
    public String resolveAndExpire(HttpServletRequest request, HttpServletResponse response) {
        String env = readEnvCookie(request);
        String url = (env != null) ? authProperties.callbackUrlFor(env) : null;
        if (url == null) {
            if (env != null) {
                log.warn("oauth_env 쿠키 값이 등록되지 않은 env, default로 fallback: env={}", env);
            }
            url = authProperties.defaultCallbackUrl();
        }
        expireEnvCookie(request, response);
        return url;
    }

    private static String readEnvCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (OAuth2EnvAwareAuthorizationRequestResolver.ENV_COOKIE_NAME.equals(c.getName())) {
                String value = c.getValue();
                return (value == null || value.isBlank()) ? null : value;
            }
        }
        return null;
    }

    private static void expireEnvCookie(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie =
                ResponseCookie.from(OAuth2EnvAwareAuthorizationRequestResolver.ENV_COOKIE_NAME, "")
                        .maxAge(0)
                        .httpOnly(true)
                        .secure(request.isSecure())
                        .sameSite("Lax")
                        .path("/")
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
