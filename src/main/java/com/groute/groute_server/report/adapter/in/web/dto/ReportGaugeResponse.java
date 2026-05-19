package com.groute.groute_server.report.adapter.in.web.dto;

import com.groute.groute_server.report.application.port.in.dto.ReportGaugeView;

import io.swagger.v3.oas.annotations.media.Schema;

/** 리포트 게이지 조회 응답 DTO. {@link ReportGaugeView}의 Web 표현. */
@Schema(description = "리포트 게이지 조회 응답")
public record ReportGaugeResponse(
        @Schema(description = "마지막 리포트 생성 이후 완료된 심화기록 수", example = "7") int currentCount,
        @Schema(description = "다음 생성 기준 수", example = "10") int nextThreshold,
        @Schema(description = "currentCount / nextThreshold. 1.0 초과 가능", example = "0.7")
                double progressRate,
        @Schema(description = "true → 리포트 생성 버튼 활성화", example = "false") boolean isGeneratable) {

    public static ReportGaugeResponse from(ReportGaugeView view) {
        return new ReportGaugeResponse(
                view.currentCount(),
                view.nextThreshold(),
                view.progressRate(),
                view.isGeneratable());
    }
}
