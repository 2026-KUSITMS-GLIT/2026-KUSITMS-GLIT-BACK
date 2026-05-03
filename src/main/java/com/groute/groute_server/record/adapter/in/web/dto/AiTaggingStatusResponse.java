package com.groute.groute_server.record.adapter.in.web.dto;

import com.groute.groute_server.record.domain.AiTaggingJob;
import com.groute.groute_server.record.domain.enums.JobStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REC-006: AI 태깅 상태 폴링 응답 DTO.
 *
 * <p>프론트는 status와 retryCount 조합으로 다음 동작을 결정한다.
 * <ul>
 *   <li>{@code FAILED && retryCount=0} → REC-005 재호출 (1차 실패, 재시도 가능)
 *   <li>{@code FAILED && retryCount=1} → 최종 실패 화면 노출
 *   <li>{@code SUCCESS} → REC-007 결과 조회
 * </ul>
 *
 * @param status     현재 잡 상태
 * @param retryCount 재시도 횟수 (0 또는 1)
 */
@Schema(description = "AI 태깅 상태 폴링 응답")
public record AiTaggingStatusResponse(

        @Schema(description = "잡 상태", example = "RUNNING")
        JobStatus status,

        @Schema(description = "재시도 횟수. 0=초기, 1=1차 실패 후 재시도 중", example = "0")
        int retryCount
) {
    public static AiTaggingStatusResponse from(AiTaggingJob job) {
        return new AiTaggingStatusResponse(job.getStatus(), job.getRetryCount());
    }
}
