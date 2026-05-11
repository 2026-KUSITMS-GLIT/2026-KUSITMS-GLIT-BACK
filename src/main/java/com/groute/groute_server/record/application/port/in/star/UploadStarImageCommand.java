package com.groute.groute_server.record.application.port.in.star;

public record UploadStarImageCommand(
        Long userId, Long starRecordId, String mimeType, Integer sizeBytes) {}