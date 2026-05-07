package com.groute.groute_server.auth.service.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.groute.groute_server.auth.config.AuthProperties;
import com.groute.groute_server.auth.dto.TokenResponse;
import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.auth.service.TokenDeliveryService;
import com.groute.groute_server.common.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 로그인 성공 시 JWT를 발급하고 프론트엔드 콜백 URL로 302 redirect.
 *
 * <p>access/refresh 발급 → refresh를 Redis에 저장 → {@link TokenDeliveryService}가 설정(쿠키/본문)에 맞게 응답을 구성.
 * 이후 query 구성은 {@link AuthProperties.RefreshToken#cookieEnabled()} 플래그를 단일 진실로 분기한다. 쿠키 모드(prod)에서는
 * deliver가 이미 Set-Cookie로 refresh를 전달했으므로 query에는 access만 싣는다. 본문 모드(local)에서는 refresh도 query에 함께
 * 실어 프론트가 즉시 사용할 수 있게 한다.
 *
 * <p>리다이렉트 대상 콜백은 {@link OAuth2EnvAwareAuthorizationRequestResolver}가 OAuth2 시작 시 발행한 {@code
 * oauth_env} 쿠키로 결정한다. 쿠키 값이 {@link AuthProperties#callback()} 화이트리스트에 있으면 그 URL로, 없거나 누락되면 default
 * env로 fallback. 사용 후에는 즉시 만료 처리(Max-Age=0)해 round-trip 흔적을 지운다.
 *
 * <p>인가 코드 교환 중 잠시 사용된 세션은 더 이상 필요 없으므로 redirect 직전에 invalidate하여 서버 상태를 JWT-only로 복귀시킨다.
 *
 * <p><b>Open redirect 안전성</b>: 콜백 URL은 {@link AuthProperties#callback()} 맵의 값에서만 선택되며 사용자 입력 URL을
 * 그대로 사용하지 않는다. redirect target은 properties로 등록된 URL 집합 외부로는 나갈 수 없다.
 *
 * <p><b>로깅 정책</b>: 토큰 값은 절대 로깅하지 않는다. userId·provider·callback URL(host+path)만 출력한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenDeliveryService tokenDeliveryService;
    private final AuthProperties authProperties;

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
        if (body.accessToken() == null) {
            // 정상 흐름에선 도달 불가. deliver 계약 위반에 대한 방어적 가드.
            throw new IllegalStateException("accessToken must not be null after deliver");
        }

        String callbackUrl = resolveCallbackUrl(request, response);
        // 쿠키 모드(prod)면 deliver가 이미 Set-Cookie로 refresh를 전달했으므로 query에는 싣지 않는다.
        String refreshForQuery =
                authProperties.refreshToken().cookieEnabled() ? null : refreshToken;
        String redirectUrl = buildRedirectUrl(callbackUrl, body.accessToken(), refreshForQuery);

        invalidateSession(request);
        log.info(
                "OAuth2 로그인 성공: userId={}, provider={}, callback={}",
                userId,
                principal.getProvider(),
                callbackUrl);
        response.sendRedirect(redirectUrl);
    }

    private String resolveCallbackUrl(HttpServletRequest request, HttpServletResponse response) {
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

    private String buildRedirectUrl(String callbackUrl, String accessToken, String refreshToken) {
        // 콜백 URL이 이미 query를 포함하면 `&`로 이어 붙여 URL이 깨지지 않게 한다.
        String separator = callbackUrl.contains("?") ? "&" : "?";
        StringBuilder sb = new StringBuilder(callbackUrl);
        sb.append(separator)
                .append("access=")
                .append(URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
        if (refreshToken != null) {
            sb.append("&refresh=").append(URLEncoder.encode(refreshToken, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
