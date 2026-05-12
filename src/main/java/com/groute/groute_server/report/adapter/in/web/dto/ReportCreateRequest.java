package com.groute.groute_server.report.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.report.application.port.in.CreateReportCommand;
import com.groute.groute_server.report.domain.enums.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;

/** 리포트 생성 요청 DTO. */
@Schema(description = "리포트 생성 요청")
public record ReportCreateRequest(
        @Schema(description = "리포트 타입", example = "MINI") ReportType reportType,
        @Schema(
                        description = "유저가 선택한 심화기록 ID 목록. MINI: 정확히 10개, CAREER: 20개 이상",
                        example = "[101, 98, 95]")
                List<Long> starRecordIds) {

    public CreateReportCommand toCommand(Long userId) {
        return new CreateReportCommand(userId, reportType, starRecordIds);
    }
}
