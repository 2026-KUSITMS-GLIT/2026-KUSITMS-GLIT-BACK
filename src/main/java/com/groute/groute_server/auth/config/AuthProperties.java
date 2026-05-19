package com.groute.groute_server.auth.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인증 관련 런타임 설정.
 *
 * <p>{@code refreshToken.cookieEnabled}는 로그인 성공 응답에서 리프레시 토큰을 어떻게 전달할지 결정한다.
 *
 * <ul>
 *   <li>{@code true}: {@code HttpOnly; Secure; SameSite=Strict} 쿠키로 전달 (HTTPS 전제, prod)
 *   <li>{@code false}: JSON 본문으로 전달 (로컬·스테이징처럼 HTTPS 보장이 어려운 환경)
 * </ul>
 *
 * <p>Secure 쿠키는 HTTPS 연결에서만 전송되므로, 로컬 HTTP 환경에서 쿠키 방식을 켜면 refresh가 전혀 흐르지 않는다. 이 분기는 그 실수를 피하기 위한
 * 것.
 *
 * <p>{@code callback}은 OAuth2 로그인 완료 후 redirect할 프론트엔드 콜백 URL을 환경(env) 키 → URL로 매핑한다. key는 프론트가
 * OAuth2 시작 시 보내는 {@code ?env=...} 값과 일치해야 한다(예: {@code local}, {@code production}). 환경별로 등록되는 key는
 * 다음과 같다.
 *
 * <ul>
 *   <li>local 프로파일: {@code local} (개발자 PC 프론트)
 *   <li>stg 프로파일: {@code local}, {@code production} (둘 다 허용)
 *   <li>prod 프로파일: {@code production} (운영 프론트만 허용)
 * </ul>
 *
 * <p>{@code defaultEnv}는 프론트가 env를 전달하지 않거나 등록되지 않은 값을 보냈을 때 fallback으로 사용할 env key. stg/prod는 SSM
 * {@code AUTH_DEFAULT_ENV}로 주입(누락 시 부팅 실패).
 *
 * <p>SSM 키 (stg/prod):
 *
 * <ul>
 *   <li>{@code AUTH_CALLBACK_LOCAL} — local env URL (stg에만)
 *   <li>{@code AUTH_CALLBACK_PRODUCTION} — production env URL
 *   <li>{@code AUTH_DEFAULT_ENV} — fallback env key
 * </ul>
 */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        RefreshToken refreshToken, Map<String, String> callback, String defaultEnv) {

    /**
     * Compact ctor — 부팅 단계 fail-fast. callback이 비어 있거나 defaultEnv가 매핑되지 않은 설정은 환경별 yaml/SSM 주입
     * 누락이므로 트래픽 들어오기 전에 즉시 노출시킨다. (런타임 첫 호출까지 늦추면 설정 사고가 배포 후에 드러남)
     */
    public AuthProperties {
        if (callback == null || callback.isEmpty()) {
            throw new IllegalStateException("auth.callback must not be empty");
        }
        if (defaultEnv == null || defaultEnv.isBlank()) {
            throw new IllegalStateException("auth.default-env must not be blank");
        }
        if (!callback.containsKey(defaultEnv)) {
            throw new IllegalStateException(
                    "auth.default-env='"
                            + defaultEnv
                            + "' is not present in auth.callback keys "
                            + callback.keySet());
        }
    }

    public record RefreshToken(boolean cookieEnabled) {}

    /** 등록된 env에 매핑된 콜백 URL. 미등록 env면 {@code null}. */
    public String callbackUrlFor(String env) {
        return callback.get(env);
    }

    /** {@link #defaultEnv}에 매핑된 콜백 URL. ctor에서 매핑 존재가 보장되므로 단순 조회. */
    public String defaultCallbackUrl() {
        return callback.get(defaultEnv);
    }
}
