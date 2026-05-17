package com.groute.groute_server.common.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 미설정 환경(로컬)에서 {@link PresignedUrlGeneratorPort}의 fallback 구현체.
 *
 * <p>{@code aws.s3.bucket}이 비어 있어 {@link S3Presigner} 빈이 등록되지 않을 때 활성화된다. API 호출 시
 * INTERNAL_SERVER_ERROR를 반환하므로 stg/prod에서는 반드시 S3 설정이 필요하다.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(S3Presigner.class)
public class NoOpPresignedUrlAdapter implements PresignedUrlGeneratorPort {

    @Override
    public PresignedUrlResult generate(String imageKey, String mimeType) {
        log.warn("[S3 미설정] Presigned URL 발급 불가 — AWS_S3_BUCKET 환경변수를 확인하세요.");
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    public void deleteObject(String imageKey) {
        log.warn("[S3 미설정] 오브젝트 삭제 불가 — AWS_S3_BUCKET 환경변수를 확인하세요.");
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
