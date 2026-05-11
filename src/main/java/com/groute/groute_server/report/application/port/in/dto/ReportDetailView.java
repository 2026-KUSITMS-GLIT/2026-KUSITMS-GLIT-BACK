package com.groute.groute_server.report.application.port.in.dto;

import java.util.Collections;
import java.util.Map;

import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportType;

/**
 * RPT-001: 리포트 상세 조회 뷰.
 *
 * <p>MINI/CAREER 타입에 따라 content 구조가 다르다.
 */
public record ReportDetailView(
        Long reportId, ReportType reportType, String createdAt, Map<String, Object> content) {

    public static ReportDetailView from(Report report) {
        String createdAt = report.getCreatedAt().toLocalDate().toString();
        Map<String, Object> content =
                report.getContentJson() != null
                        ? Collections.unmodifiableMap(report.getContentJson())
                        : Collections.emptyMap();
        return new ReportDetailView(report.getId(), report.getReportType(), createdAt, content);
    }
}
