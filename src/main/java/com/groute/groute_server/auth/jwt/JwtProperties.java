package com.groute.groute_server.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 서명 키 및 토큰 만료 시간 설정.
 *
 * <p>환경별 값은 SSM Parameter Store의 {@code /groute/{env}/JWT_SECRET}에서 주입된다.
 *
 * <p>만료 시간 단위는 밀리초(ms). HS256 서명을 위해 {@code secret}은 32바이트(UTF-8) 이상이어야 한다.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret, long accessTokenExpiration, long refreshTokenExpiration) {}
