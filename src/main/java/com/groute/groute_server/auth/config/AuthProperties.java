package com.groute.groute_server.auth.config;

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
 * <p>{@code frontCallbackUrl}은 OAuth2 로그인 완료 후 리다이렉트할 프론트엔드 콜백 URL이다. 환경별로 다음 값을 사용하며, stg/prod는
 * SSM에서 {@code AUTH_FRONT_CALLBACK_URL} 키로 주입한다(누락 시 부팅 실패로 설정 오류 즉시 노출).
 *
 * <ul>
 *   <li>로컬: {@code http://localhost:3000/auth/callback}
 *   <li>운영: {@code https://glit.today/auth/callback}
 * </ul>
 */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(RefreshToken refreshToken, String frontCallbackUrl) {

    public record RefreshToken(boolean cookieEnabled) {}
}
