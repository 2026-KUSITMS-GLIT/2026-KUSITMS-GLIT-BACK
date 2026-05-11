package com.groute.groute_server.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS S3 Presigned URL 설정.
 *
 * <p>stg/prod는 SSM에서 AWS_S3_BUCKET, AWS_S3_CDN_BASE_URL 주입 필수 — 누락 시 부팅 실패(fail-fast). 로컬은
 * 하단 local 프로파일에서 빈 문자열 fallback 허용 (이미지 업로드 비활성).
 */
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        String bucket, String region, long presignedUrlExpirationMinutes, String cdnBaseUrl) {}