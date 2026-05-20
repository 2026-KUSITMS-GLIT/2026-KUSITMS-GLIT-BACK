package com.groute.groute_server.record.adapter.in.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.groute.groute_server.record.application.port.in.star.UploadStarImageCommand;

public record UploadStarImageRequest(
        @NotEmpty @Size(max = 2) List<@NotBlank @Pattern(regexp = "image/jpeg|image/png") String> mimeTypes) {

    public UploadStarImageCommand toCommand(Long userId, Long starRecordId) {
        return new UploadStarImageCommand(userId, starRecordId, mimeTypes);
    }
}
