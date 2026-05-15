package com.groute.groute_server.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 온보딩 완료 여부 조회(ONB-STATUS) 응답 DTO. */
public record OnboardingStatusResponse(
        @Schema(description = "온보딩 완료 여부", example = "true") boolean isOnboardingCompleted) {}
