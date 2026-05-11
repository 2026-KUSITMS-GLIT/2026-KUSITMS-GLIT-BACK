package com.groute.groute_server.report.adapter.in.web.dto;

import java.util.Map;

import com.groute.groute_server.report.application.port.in.dto.ReportDetailView;
import com.groute.groute_server.report.domain.enums.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;

/** 리포트 상세 조회 응답 DTO. {@link ReportDetailView}의 Web 표현. */
@Schema(description = "리포트 상세 조회 응답")
public record ReportDetailResponse(
        @Schema(description = "리포트 식별자", example = "1") Long reportId,
        @Schema(description = "리포트 종류", example = "CAREER") ReportType reportType,
        @Schema(description = "리포트 생성일 (yyyy-MM-dd)", example = "2026-04-10") String createdAt,
        @Schema(description = "리포트 본문. MINI/CAREER 타입별 구조 상이") Map<String, Object> content) {

    public static ReportDetailResponse from(ReportDetailView view) {
        return new ReportDetailResponse(
                view.reportId(), view.reportType(), view.createdAt(), view.content());
    }
}
