package com.groute.groute_server.record.adapter.in.web.dto;

import com.groute.groute_server.record.application.port.in.star.UploadStarImageResult;

public record UploadStarImageResponse(Long imageId, String presignedUrl, String imageUrl) {

    public static UploadStarImageResponse from(UploadStarImageResult result) {
        return new UploadStarImageResponse(
                result.imageId(), result.presignedUrl(), result.imageUrl());
    }
}
