package com.groute.groute_server.report.application.port.in.dto;

import java.util.List;

import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportStatus;
import com.groute.groute_server.report.domain.enums.ReportType;

/**
 * RPT-001: 리포트 목록 조회 뷰.
 *
 * <p>생성일 기준 내림차순 정렬. 이력 없으면 빈 배열.
 */
public record ReportListView(List<ReportItemView> reports) {

    public static ReportListView from(List<Report> reports) {
        return new ReportListView(reports.stream().map(ReportItemView::from).toList());
    }

    public record ReportItemView(
            Long reportId,
            ReportType reportType,
            ReportStatus status,
            String createdAt,
            String title,
            String previewText,
            String competencyStatSummary) {

        public static ReportItemView from(Report report) {
            String createdAt = report.getCreatedAt().toLocalDate().toString();

            String previewText = null;
            String competencyStatSummary = null;

            if (report.getContentJson() != null) {
                Object previewRaw =
                        report.getReportType() == ReportType.CAREER
                                ? report.getContentJson().get("narrativeSummary")
                                : report.getContentJson().get("activitySummary");
                previewText = previewRaw instanceof String s ? s : null;

                if (report.getReportType() == ReportType.MINI) {
                    Object summaryRaw = report.getContentJson().get("competencyStatSummary");
                    competencyStatSummary = summaryRaw instanceof String s ? s : null;
                }
            }

            return new ReportItemView(
                    report.getId(),
                    report.getReportType(),
                    report.getStatus(),
                    createdAt,
                    report.getTitle(),
                    previewText,
                    competencyStatSummary);
        }
    }
}
