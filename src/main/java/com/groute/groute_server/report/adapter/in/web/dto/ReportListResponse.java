package com.groute.groute_server.report.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.report.application.port.in.dto.ReportListView;
import com.groute.groute_server.report.domain.enums.ReportStatus;
import com.groute.groute_server.report.domain.enums.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;

/** 리포트 목록 조회 응답 DTO. {@link ReportListView}의 Web 표현. */
@Schema(description = "리포트 목록 조회 응답")
public record ReportListResponse(
        @Schema(description = "리포트 목록. 생성일 기준 내림차순. 이력 없으면 빈 배열")
                List<ReportItemResponse> reports) {

    public static ReportListResponse from(ReportListView view) {
        return new ReportListResponse(
                view.reports().stream().map(ReportItemResponse::from).toList());
    }

    @Schema(description = "리포트 목록 카드 아이템")
    public record ReportItemResponse(
            @Schema(description = "리포트 식별자", example = "1") Long reportId,
            @Schema(description = "리포트 종류", example = "CAREER") ReportType reportType,
            @Schema(description = "리포트 상태", example = "SUCCESS") ReportStatus status,
            @Schema(description = "리포트 생성일 (yyyy.MM.dd)", example = "2026.04.10") String createdAt,
            @Schema(
                            description =
                                    "리포트 제목. MINI: '{닉네임}님은 어떤 {직군명}으로 성장하고 있을까요?' 고정 문구."
                                            + " CAREER: AI 생성 브랜딩 문장 (reports.title)",
                            example = "박도현님은 어떤 개발자로 성장하고 있을까요?")
                    String title,
            @Schema(description = "목록 카드 텍스트 프리뷰") String previewText,
            @Schema(
                            description = "MINI 타입만 존재. 역량 기록 현황 1줄 요약",
                            example = "가장 많이 기록한 영역은 기획·실행(6회)")
                    String competencyStatSummary) {

        public static ReportItemResponse from(ReportListView.ReportItemView view) {
            return new ReportItemResponse(
                    view.reportId(),
                    view.reportType(),
                    view.status(),
                    view.createdAt(),
                    view.title(),
                    view.previewText(),
                    view.competencyStatSummary());
        }
    }
}
