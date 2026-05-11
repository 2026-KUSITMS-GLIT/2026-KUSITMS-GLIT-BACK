package com.groute.groute_server.common.storage;

/** S3 Presigned PUT URL 생성 포트. */
public interface PresignedUrlGeneratorPort {

    PresignedUrlResult generate(String imageKey, String mimeType);
}