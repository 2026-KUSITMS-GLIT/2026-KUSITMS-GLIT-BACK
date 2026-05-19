package com.groute.groute_server.common.storage;

/** S3 Presigned URL 발급 결과. */
public record PresignedUrlResult(String presignedUrl, String imageUrl) {}
