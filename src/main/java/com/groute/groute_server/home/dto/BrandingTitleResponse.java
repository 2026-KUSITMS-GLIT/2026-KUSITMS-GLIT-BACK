package com.groute.groute_server.home.dto;

import org.springframework.lang.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

public record BrandingTitleResponse(
        @Schema(description = "직무 브랜딩 문구. 신규 사용자는 null", nullable = true) @Nullable
                String brandingTitle) {

    public static BrandingTitleResponse from(String brandingTitle) {
        return new BrandingTitleResponse(brandingTitle);
    }
}
