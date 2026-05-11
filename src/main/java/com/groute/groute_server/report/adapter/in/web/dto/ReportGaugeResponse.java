package com.groute.groute_server.report.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * RPT-001: 리포트 게이지 조회 응답.
 *
 * <p>마지막 리포트 생성 이후 완료된 심화기록 수와 다음 생성 임계치를 반환한다. progressRate는 1.0 초과 가능 (10회 초과 시에도 카운팅 계속).
 */
@Schema(description = "리포트 게이지 조회 응답")
public record ReportGaugeResponse(
        @Schema(description = "마지막 리포트 생성 이후 완료된 심화기록 수", example = "7") int currentCount,
        @Schema(description = "다음 생성 기준 수", example = "10") int nextThreshold,
        @Schema(description = "currentCount / nextThreshold. 1.0 초과 가능", example = "0.7")
                double progressRate,
        @Schema(description = "true → 리포트 생성 버튼 활성화", example = "false") boolean isGeneratable) {

    public static ReportGaugeResponse of(int currentCount, int nextThreshold) {
        double progressRate = (double) currentCount / nextThreshold;
        boolean isGeneratable = currentCount >= nextThreshold;
        return new ReportGaugeResponse(currentCount, nextThreshold, progressRate, isGeneratable);
    }
}
