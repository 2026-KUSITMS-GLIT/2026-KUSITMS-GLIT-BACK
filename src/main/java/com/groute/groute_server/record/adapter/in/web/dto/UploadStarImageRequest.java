package com.groute.groute_server.record.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import com.groute.groute_server.record.application.port.in.star.UploadStarImageCommand;

public record UploadStarImageRequest(
        @NotBlank @Pattern(regexp = "image/jpeg|image/png|image/webp") String mimeType,
        @NotNull @Positive @Max(10_485_760) Integer sizeBytes) {

    public UploadStarImageCommand toCommand(Long userId, Long starRecordId) {
        return new UploadStarImageCommand(userId, starRecordId, mimeType, sizeBytes);
    }
}
