package com.groute.groute_server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 재발급 요청 본문 (쿠키 모드가 아닌 환경에서만 사용).
 *
 * <p>쿠키 모드(prod)는 {@code refreshToken} 쿠키에서 값을 읽으므로 본문이 필요 없다. 로컬·스테이징처럼 쿠키를 안 쓰는 환경에서는 이 DTO로 값을
 * 전달한다. 컨트롤러가 쿠키 우선, 본문 폴백 순서로 조회.
 */
@Schema(description = "리프레시 토큰 재발급 요청")
public record TokenReissueRequest(@Schema(description = "리프레시 토큰") String refreshToken) {}
