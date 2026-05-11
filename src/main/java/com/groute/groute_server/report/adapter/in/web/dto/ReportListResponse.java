package com.groute.groute_server.report.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportStatus;
import com.groute.groute_server.report.domain.enums.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * RPT-001: 리포트 목록 조회 응답.
 *
 * <p>생성일 기준 내림차순 정렬. 이력 없으면 빈 배열 반환.
 */
@Schema(description = "리포트 목록 조회 응답")
public record ReportListResponse(
        @Schema(description = "리포트 목록. 생성일 기준 내림차순. 이력 없으면 빈 배열") List<ReportItem> reports) {

    public static ReportListResponse from(List<Report> reports) {
        return new ReportListResponse(reports.stream().map(ReportItem::from).toList());
    }

    @Schema(description = "리포트 목록 카드 아이템")
    public record ReportItem(
            @Schema(description = "리포트 식별자", example = "1") Long reportId,
            @Schema(description = "리포트 종류", example = "CAREER") ReportType reportType,
            @Schema(description = "리포트 상태", example = "SUCCESS") ReportStatus status,
            @Schema(description = "리포트 생성일 (yyyy-MM-dd)", example = "2026-04-10") String createdAt,
            @Schema(
                            description = "커리어 브랜딩 문장. CAREER 타입만 존재",
                            example = "OOO님은 복잡한 문제를 구조로 풀어내는 '설계형 기획자'입니다.")
                    String title,
            @Schema(description = "목록 카드 텍스트 프리뷰. CAREER는 통합 서사 요약 앞부분, MINI는 활동 요약 앞부분")
                    String previewText,
            @Schema(
                            description = "MINI 타입만 존재. 역량 기록 현황 1줄 요약",
                            example = "가장 많이 기록한 영역은 기획·실행(6회)")
                    String competencyStatSummary) {

        public static ReportItem from(Report report) {
            String createdAt = report.getCreatedAt().toLocalDate().toString(); // yyyy-MM-dd

            // content_json에서 previewText, competencyStatSummary 추출
            String previewText = null;
            String competencyStatSummary = null;

            if (report.getContentJson() != null) {
                if (report.getReportType() == ReportType.CAREER) {
                    previewText = (String) report.getContentJson().get("narrativeSummary");
                } else {
                    previewText = (String) report.getContentJson().get("activitySummary");
                    competencyStatSummary =
                            (String) report.getContentJson().get("competencyStatSummary");
                }
            }

            return new ReportItem(
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
