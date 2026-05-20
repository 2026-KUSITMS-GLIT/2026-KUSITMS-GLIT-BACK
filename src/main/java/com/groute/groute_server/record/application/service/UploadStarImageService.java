package com.groute.groute_server.record.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.common.storage.PresignedUrlResult;
import com.groute.groute_server.record.application.port.in.star.UploadStarImageCommand;
import com.groute.groute_server.record.application.port.in.star.UploadStarImageResult;
import com.groute.groute_server.record.application.port.in.star.UploadStarImageUseCase;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.StarRecord;

import lombok.RequiredArgsConstructor;

/** STAR 이미지 업로드 Presigned URL 발급 서비스 (POST /api/star-records/{id}/images/presigned-url). */
@Service
@RequiredArgsConstructor
@Transactional
public class UploadStarImageService implements UploadStarImageUseCase {

    private static final int MAX_IMAGES_PER_STAR = 2;

    private final StarRecordRepositoryPort starRecordRepositoryPort;
    private final StarImageQueryPort starImageQueryPort;
    private final PresignedUrlGeneratorPort presignedUrlGeneratorPort;

    @Override
    public List<UploadStarImageResult> upload(UploadStarImageCommand command) {
        StarRecord record =
                starRecordRepositoryPort
                        .findByIdWithLock(command.starRecordId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_NOT_FOUND));

        if (!record.isOwnedBy(command.userId())) {
            throw new BusinessException(ErrorCode.STAR_FORBIDDEN);
        }

        if (record.isWriteLocked()) {
            throw new BusinessException(ErrorCode.STAR_WRITE_LOCKED);
        }

        int existingCount =
                starImageQueryPort
                        .findAllByStarRecordIdOrderBySortOrder(command.starRecordId())
                        .size();
        if (existingCount + command.mimeTypes().size() > MAX_IMAGES_PER_STAR) {
            throw new BusinessException(ErrorCode.STAR_IMAGE_LIMIT_EXCEEDED);
        }

        return command.mimeTypes().stream()
                .map(
                        mimeType -> {
                            String imageKey =
                                    String.format(
                                            "star-images/%d/%d/%s.%s",
                                            command.userId(),
                                            command.starRecordId(),
                                            UUID.randomUUID(),
                                            toExtension(mimeType));
                            PresignedUrlResult presigned =
                                    presignedUrlGeneratorPort.generate(imageKey, mimeType);
                            return new UploadStarImageResult(
                                    imageKey, presigned.presignedUrl(), presigned.imageUrl());
                        })
                .toList();
    }

    private String toExtension(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }
}
