package com.groute.groute_server.auth.service.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.groute.groute_server.auth.config.AuthProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 시작 시 프론트 시작 origin(env)을 짧은 수명 쿠키로 보존한다.
 *
 * <p>프론트가 {@code GET /oauth2/authorization/{provider}?env=local|production}으로 호출하면, 이 resolver가 env
 * 값을 {@link AuthProperties#callback()} 화이트리스트와 매칭해 쿠키 {@code oauth_env}로 저장한다. 이후 provider redirect
 * → callback → {@code OAuth2LoginSuccessHandler}가 쿠키에서 env를 추출해 적절한 콜백 URL을 선택한다(후속 작업 C).
 *
 * <p>env 값이 누락되거나 화이트리스트에 없으면 쿠키를 설정하지 않으며 SuccessHandler가 default env로 fallback한다.
 *
 * <p><b>SameSite=Lax 선택 이유</b>: OAuth2 provider에서 우리 도메인으로 돌아오는 redirect는 cross-site top-level
 * navigation이다. {@code Strict}는 그 케이스에 쿠키를 전송하지 않으므로, OAuth2 round-trip을 살리려면 {@code Lax}가 적정.
 *
 * <p><b>Secure 플래그</b>: {@link HttpServletRequest#isSecure()} 결과를 따른다. 로컬 HTTP에서는 false (브라우저가
 * secure 쿠키를 거부하지 않게), HTTPS 환경(stg/prod)에서는 true.
 */
@Slf4j
public class OAuth2EnvAwareAuthorizationRequestResolver
        implements OAuth2AuthorizationRequestResolver {

    static final String ENV_COOKIE_NAME = "oauth_env";
    static final String ENV_QUERY_PARAM = "env";
    static final int COOKIE_MAX_AGE_SECONDS = 300;

    private final OAuth2AuthorizationRequestResolver delegate;
    private final AuthProperties authProperties;

    public OAuth2EnvAwareAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            AuthProperties authProperties) {
        this.delegate =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");
        this.authProperties = authProperties;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest result = delegate.resolve(request);
        if (result != null) {
            captureEnvCookie(request);
        }
        return result;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest result = delegate.resolve(request, clientRegistrationId);
        if (result != null) {
            captureEnvCookie(request);
        }
        return result;
    }

    private void captureEnvCookie(HttpServletRequest request) {
        String env = request.getParameter(ENV_QUERY_PARAM);
        if (env == null || env.isBlank()) {
            return;
        }
        if (!authProperties.callback().containsKey(env)) {
            log.warn("OAuth2 시작에서 미등록 env 수신, 쿠키 설정 스킵: env={}", env);
            return;
        }
        HttpServletResponse response = currentResponse();
        if (response == null) {
            return;
        }

        ResponseCookie cookie =
                ResponseCookie.from(ENV_COOKIE_NAME, env)
                        .httpOnly(true)
                        .secure(request.isSecure())
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(COOKIE_MAX_AGE_SECONDS)
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static HttpServletResponse currentResponse() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getResponse();
        }
        return null;
    }
}
