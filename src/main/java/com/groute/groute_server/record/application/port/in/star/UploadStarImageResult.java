package com.groute.groute_server.record.application.port.in.star;

public record UploadStarImageResult(Long imageId, String presignedUrl, String imageUrl) {}
