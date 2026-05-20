package com.groute.groute_server.record.application.port.in.star;

public record UploadStarImageResult(String imageKey, String presignedUrl, String imageUrl) {}

