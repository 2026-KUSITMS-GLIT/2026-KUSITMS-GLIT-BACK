package com.groute.groute_server.home.dto;

import java.util.Map;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

import io.swagger.v3.oas.annotations.media.Schema;

public record RadarResponse(
        @Schema(description = "정규화용 min값") int min,
        @Schema(description = "정규화용 max값") int max,
        @Schema(description = "카테고리별 완료 STAR 건수") Map<CompetencyCategory, Integer> categories) {

    public static RadarResponse from(RadarResult result) {
        return new RadarResponse(result.min(), result.max(), result.categories());
    }
}
