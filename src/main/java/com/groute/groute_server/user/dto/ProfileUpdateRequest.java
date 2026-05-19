package com.groute.groute_server.user.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 마이페이지 프로필 수정(MYP002) 요청 DTO.
 *
 * <p>요청 바디는 항상 두 필드를 모두 포함한다(스펙). 값은 한글 라벨 문자열로 받아 서비스 계층에서 {@code JobRole.fromLabel} / {@code
 * UserStatus.fromLabel}로 enum 변환한다.
 */
public record ProfileUpdateRequest(
        @NotBlank(message = "직군은 필수입니다.")
                @Schema(description = "유저 직군 (기획자 | 개발자 | 디자이너)", example = "개발자")
                String jobRole,
        @NotBlank(message = "사용자 상태는 필수입니다.")
                @Schema(description = "유저 상태 (재학 중 | 취업 준비 중 | 재직 중)", example = "재학 중")
                String userStatus) {}
