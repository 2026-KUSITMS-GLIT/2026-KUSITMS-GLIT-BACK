package com.groute.groute_server.record.adapter.in.web.dto;

import com.groute.groute_server.record.application.port.in.star.UploadStarImageResult;

public record UploadStarImageResponse(String imageKey, String presignedUrl, String imageUrl) {

    public static UploadStarImageResponse from(UploadStarImageResult result) {
        return new UploadStarImageResponse(
                result.imageKey(), result.presignedUrl(), result.imageUrl());
    }
}
