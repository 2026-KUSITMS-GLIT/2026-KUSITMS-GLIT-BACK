package com.groute.groute_server.record.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

import com.groute.groute_server.record.application.port.in.star.ConfirmStarImageCommand;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmStarImageRequest(
        @Schema(description = "Presigned URL 발급 시 반환된 imageKey") @NotBlank String imageKey) {

    public ConfirmStarImageCommand toCommand(Long userId, Long starRecordId) {
        return new ConfirmStarImageCommand(userId, starRecordId, imageKey);
    }
}
