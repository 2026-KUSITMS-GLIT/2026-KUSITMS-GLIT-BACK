package com.groute.groute_server.record.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import com.groute.groute_server.record.application.port.in.star.UploadStarImageCommand;

public record UploadStarImageRequest(
        @NotBlank @Pattern(regexp = "image/jpeg|image/png") String mimeType) {

    public UploadStarImageCommand toCommand(Long userId, Long starRecordId) {
        return new UploadStarImageCommand(userId, starRecordId, mimeType);
    }
}
