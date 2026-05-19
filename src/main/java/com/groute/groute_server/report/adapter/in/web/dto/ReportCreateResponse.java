package com.groute.groute_server.report.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 리포트 생성 요청 및 재시도 응답 DTO. */
@Schema(description = "리포트 생성 응답")
public record ReportCreateResponse(
        @Schema(description = "생성된 리포트 식별자", example = "3") Long reportId) {

    public static ReportCreateResponse from(Long reportId) {
        return new ReportCreateResponse(reportId);
    }
}
