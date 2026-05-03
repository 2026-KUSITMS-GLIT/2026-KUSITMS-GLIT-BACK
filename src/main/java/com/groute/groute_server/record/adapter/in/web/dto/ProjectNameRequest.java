package com.groute.groute_server.record.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/** 프로젝트 태그 생성/수정 요청 DTO. */
public record ProjectNameRequest(
        @NotBlank(message = "프로젝트 태그 이름은 필수입니다.")
                @Size(max = 15, message = "15자 이상 입력할 수 없어요.")
                @Schema(description = "프로젝트 태그 이름 (최대 15자)", example = "2026 U-KATHON")
                String name) {}
