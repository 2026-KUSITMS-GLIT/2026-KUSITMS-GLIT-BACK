package com.groute.groute_server.record.adapter.in.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.groute.groute_server.record.application.port.in.star.ConfirmStarImageCommand;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmStarImageRequest(
        @Schema(description = "Presigned URL 발급 시 반환된 imageKey 목록") @NotEmpty @Size(max = 2)
                List<@NotBlank String> imageKeys) {

    public ConfirmStarImageCommand toCommand(Long userId, Long starRecordId) {
        return new ConfirmStarImageCommand(userId, starRecordId, imageKeys);
    }
}
