package com.groute.groute_server.record.application.port.in.star;

public record ConfirmStarImageCommand(
        Long userId, Long starRecordId, String imageKey, String mimeType, Integer sizeBytes) {}
