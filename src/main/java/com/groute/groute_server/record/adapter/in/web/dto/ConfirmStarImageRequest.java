package com.groute.groute_server.record.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import com.groute.groute_server.record.application.port.in.star.ConfirmStarImageCommand;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmStarImageRequest(
        @Schema(description = "Presigned URL 발급 시 반환된 imageKey") @NotBlank String imageKey,
        @Schema(description = "이미지 MIME 타입", example = "image/jpeg")
                @NotBlank
                @Pattern(regexp = "image/jpeg|image/png|image/webp")
                String mimeType,
        @Schema(description = "파일 크기 (bytes, 최대 10MB)", example = "204800")
                @NotNull
                @Positive
                @Max(10_485_760)
                Integer sizeBytes) {

    public ConfirmStarImageCommand toCommand(Long userId, Long starRecordId) {
        return new ConfirmStarImageCommand(userId, starRecordId, imageKey, mimeType, sizeBytes);
    }
}