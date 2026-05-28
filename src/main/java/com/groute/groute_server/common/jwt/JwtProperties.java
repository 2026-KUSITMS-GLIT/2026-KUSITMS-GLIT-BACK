package com.groute.groute_server.common.jwt;

import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 서명 키 및 토큰 만료 시간 설정.
 *
 * <p>환경별 값은 SSM Parameter Store의 {@code /groute/{env}/JWT_SECRET}에서 주입된다.
 *
 * <p>만료 시간 단위는 밀리초(ms). HS256 서명을 위해 {@code secret}은 32바이트(UTF-8) 이상이어야 하며, 미충족 시 부팅 단계에서 실패한다.
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @Size(min = 32, message = "jwt.secret은 HS256 서명을 위해 32바이트 이상이어야 한다") String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration) {}
