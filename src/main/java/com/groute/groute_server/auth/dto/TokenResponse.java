package com.groute.groute_server.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인/재발급 성공 시 반환되는 토큰 페이로드.
 *
 * <p>쿠키 모드(prod)에서는 {@code refreshToken}이 HttpOnly 쿠키로만 내려가고 본문에서는 생략되어 null → JSON 직렬화에서 제외된다.
 */
@Schema(description = "토큰 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        @Schema(description = "액세스 토큰") String accessToken,
        @Schema(description = "리프레시 토큰 (쿠키 모드에서는 null)") String refreshToken) {}
