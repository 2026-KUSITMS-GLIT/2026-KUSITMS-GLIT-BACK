package com.groute.groute_server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 마이페이지 프로필 수정(MYP002) 요청 DTO.
 *
 * <p>요청 바디는 항상 세 필드를 모두 포함한다(스펙). 직군·상태 라벨은 한글 문자열로 받아 서비스 계층에서 {@code JobRole.fromLabel} / {@code
 * UserStatus.fromLabel}로 enum 변환한다. 닉네임은 ONB003과 동일한 제약(2~12자, 한글·영문·숫자)을 따른다.
 */
public record ProfileUpdateRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
                @Size(min = 2, max = 12, message = "닉네임은 2자 이상 12자 이하여야 합니다.")
                @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
                @Schema(description = "닉네임 (2~12자, 한글·영문·숫자)", example = "겨레")
                String nickname,
        @NotBlank(message = "직군은 필수입니다.")
                @Schema(description = "유저 직군 (기획자 | 개발자 | 디자이너)", example = "개발자")
                String jobRole,
        @NotBlank(message = "사용자 상태는 필수입니다.")
                @Schema(description = "유저 상태 (재학 중 | 취업 준비 중 | 재직 중)", example = "재학 중")
                String userStatus) {}
