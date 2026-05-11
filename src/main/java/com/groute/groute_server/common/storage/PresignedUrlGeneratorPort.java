package com.groute.groute_server.common.storage;

/** S3 오브젝트 스토리지 포트. Presigned PUT URL 생성 및 오브젝트 삭제를 담당한다. */
public interface PresignedUrlGeneratorPort {

    PresignedUrlResult generate(String imageKey, String mimeType);

    void deleteObject(String imageKey);
}