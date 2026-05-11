package com.groute.groute_server.record.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.groute.groute_server.record.application.port.in.star.UpdateStarRecordStepCommand;
import com.groute.groute_server.record.domain.enums.StarStep;

import io.swagger.v3.oas.annotations.media.Schema;

/** STAR 단계별 저장 요청 DTO. */
@Schema(description = "STAR 단계 저장 요청")
public record StarRecordStepUpdateRequest(
        @NotBlank
                @Size(max = 300, message = "내용은 최대 300자입니다.")
                @Schema(description = "단계 작성 내용 (최대 300자)")
                String userAnswer) {

    public UpdateStarRecordStepCommand toCommand(Long userId, Long starRecordId, StarStep step) {
        return new UpdateStarRecordStepCommand(userId, starRecordId, step, userAnswer);
    }
}
