package com.groute.groute_server.report.adapter.in.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.groute.groute_server.report.application.port.in.CreateReportCommand;
import com.groute.groute_server.report.domain.enums.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;

/** 리포트 생성 요청 DTO. */
@Schema(description = "리포트 생성 요청")
public record ReportCreateRequest(
        @NotNull(message = "리포트 타입은 필수입니다.") @Schema(description = "리포트 타입", example = "MINI")
                ReportType reportType,
        @NotEmpty(message = "심화기록 ID 목록은 필수입니다.")
                @Schema(
                        description = "유저가 선택한 심화기록 ID 목록. MINI: 정확히 10개, CAREER: 20개 이상",
                        example = "[101, 98, 95]")
                List<Long> starRecordIds) {

    public CreateReportCommand toCommand(Long userId) {
        return new CreateReportCommand(userId, reportType, starRecordIds);
    }
}
