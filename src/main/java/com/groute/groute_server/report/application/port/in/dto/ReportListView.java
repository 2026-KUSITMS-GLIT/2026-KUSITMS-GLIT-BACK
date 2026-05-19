package com.groute.groute_server.report.application.port.in.dto;

import java.time.ZoneId;
import java.util.List;

import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportStatus;
import com.groute.groute_server.report.domain.enums.ReportType;
import com.groute.groute_server.user.entity.User;

/**
 * RPT-001: 리포트 목록 조회 뷰.
 *
 * <p>생성일 기준 내림차순 정렬. 이력 없으면 빈 배열.
 */
public record ReportListView(List<ReportItemView> reports) {

    public static ReportListView from(List<Report> reports, User user) {
        return new ReportListView(reports.stream().map(r -> ReportItemView.from(r, user)).toList());
    }

    public record ReportItemView(
            Long reportId,
            ReportType reportType,
            ReportStatus status,
            String createdAt,
            String title,
            String previewText,
            String competencyStatSummary) {

        public static ReportItemView from(Report report, User user) {
            String createdAt =
                    report.getCreatedAt()
                            .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                            .toLocalDate()
                            .toString()
                            .replace('-', '.');

            String title =
                    report.getReportType() == ReportType.MINI
                            ? user.getNickname()
                                    + "님은 어떤 "
                                    + user.getJobRole().getLabel()
                                    + "으로 성장하고 있을까요?"
                            : report.getTitle();

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
                    title,
                    previewText,
                    competencyStatSummary);
        }
    }
}
