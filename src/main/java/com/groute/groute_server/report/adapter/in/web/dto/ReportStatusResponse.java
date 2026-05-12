package com.groute.groute_server.report.adapter.in.web.dto;

import com.groute.groute_server.report.application.port.in.ReportStatusView;

import io.swagger.v3.oas.annotations.media.Schema;

/** 리포트 생성 상태 폴링 응답 DTO. {@link ReportStatusView}의 Web 표현. */
@Schema(description = "리포트 생성 상태 응답")
public record ReportStatusResponse(
        @Schema(description = "리포트 식별자", example = "3") Long reportId,
        @Schema(
                        description = "리포트 생성 상태. SUCCESS 시 상세 조회, FAILED 시 재시도 버튼 노출",
                        example = "GENERATING")
                String status,
        @Schema(description = "재시도 가능 여부. FAILED 상태이고 아직 재시도하지 않은 경우에만 true", example = "true")
                Boolean retryAvailable) {

    public static ReportStatusResponse from(ReportStatusView view) {
        return new ReportStatusResponse(view.reportId(), view.status(), view.retryAvailable());
    }
}
