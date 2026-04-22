package com.groute.groute_server.auth.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 재발급 요청 본문 (쿠키 모드가 아닌 환경에서만 사용).
 *
 * <p>쿠키 모드(prod)는 {@code refreshToken} 쿠키에서 값을 읽으므로 본문이 필요 없다. 로컬·스테이징처럼 쿠키를 안 쓰는 환경에서는 이 DTO로 값을
 * 전달한다. 컨트롤러가 쿠키 우선, 본문 폴백 순서로 조회.
 *
 * <p>{@link NotBlank} 검증으로 빈 문자열·공백을 API 경계에서 차단. 토큰은 민감 정보라 {@code accessMode = WRITE_ONLY}로 선언해
 * 응답 스키마에는 노출되지 않도록 한다.
 */
@Schema(description = "리프레시 토큰 재발급 요청")
public record TokenReissueRequest(
        @Schema(description = "리프레시 토큰", accessMode = Schema.AccessMode.WRITE_ONLY)
                @NotBlank(message = "리프레시 토큰은 비어 있을 수 없습니다.")
                String refreshToken) {}
