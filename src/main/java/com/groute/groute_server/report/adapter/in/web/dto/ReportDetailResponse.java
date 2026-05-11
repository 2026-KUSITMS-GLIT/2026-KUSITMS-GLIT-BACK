package com.groute.groute_server.report.adapter.in.web.dto;

import java.util.Map;

import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * RPT-001: 리포트 상세 조회 응답.
 *
 * <p>MINI/CAREER 타입에 따라 content 구조가 다르다. content는 content_json을 그대로 반환하며, 역직렬화는 이슈 2(생성) 구조 확정 후
 * 처리한다.
 */
@Schema(description = "리포트 상세 조회 응답")
public record ReportDetailResponse(
        @Schema(description = "리포트 식별자", example = "1") Long reportId,
        @Schema(description = "리포트 종류", example = "CAREER") ReportType reportType,
        @Schema(description = "리포트 생성일 (yyyy-MM-dd)", example = "2026-04-10") String createdAt,
        @Schema(description = "리포트 본문. MINI/CAREER 타입별 구조 상이") Map<String, Object> content) {

    public static ReportDetailResponse from(Report report) {
        String createdAt = report.getCreatedAt().toLocalDate().toString();
        return new ReportDetailResponse(
                report.getId(), report.getReportType(), createdAt, report.getContentJson());
    }
}
