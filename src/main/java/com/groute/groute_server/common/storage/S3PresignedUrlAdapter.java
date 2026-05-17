package com.groute.groute_server.common.storage;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.groute.groute_server.common.config.S3Properties;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * {@link PresignedUrlGeneratorPort}의 AWS S3 구현체.
 *
 * <p>{@link com.groute.groute_server.common.config.S3Config}가 등록된 경우에만 활성화된다. 로컬에서 {@code
 * aws.s3.bucket}이 비어 있으면 S3Presigner 빈이 없어 이 어댑터도 등록되지 않는다.
 */
@Component
@ConditionalOnBean(S3Presigner.class)
@RequiredArgsConstructor
public class S3PresignedUrlAdapter implements PresignedUrlGeneratorPort {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final S3Properties properties;

    @Override
    public PresignedUrlResult generate(String imageKey, String mimeType) {
        PutObjectRequest putRequest =
                PutObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(imageKey)
                        .contentType(mimeType)
                        .build();

        String presignedUrl =
                s3Presigner
                        .presignPutObject(
                                PutObjectPresignRequest.builder()
                                        .putObjectRequest(putRequest)
                                        .signatureDuration(
                                                Duration.ofMinutes(
                                                        properties.presignedUrlExpirationMinutes()))
                                        .build())
                        .url()
                        .toString();

        String imageUrl = properties.cdnBaseUrl() + "/" + imageKey;
        return new PresignedUrlResult(presignedUrl, imageUrl);
    }

    @Override
    public void deleteObject(String imageKey) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(properties.bucket()).key(imageKey).build());
    }
}
