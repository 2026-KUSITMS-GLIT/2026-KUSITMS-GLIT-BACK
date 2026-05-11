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
import com.groute.groute_server.record.application.port.out.star.StarImageWritePort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.StarImage;
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
    private final StarImageWritePort starImageWritePort;
    private final PresignedUrlGeneratorPort presignedUrlGeneratorPort;

    @Override
    public UploadStarImageResult upload(UploadStarImageCommand command) {
        StarRecord record =
                starRecordRepositoryPort
                        .findById(command.starRecordId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_NOT_FOUND));

        if (!record.isOwnedBy(command.userId())) {
            throw new BusinessException(ErrorCode.STAR_FORBIDDEN);
        }

        if (record.isWriteLocked()) {
            throw new BusinessException(ErrorCode.STAR_WRITE_LOCKED);
        }

        List<StarImage> existing =
                starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(command.starRecordId());
        if (existing.size() >= MAX_IMAGES_PER_STAR) {
            throw new BusinessException(ErrorCode.STAR_IMAGE_LIMIT_EXCEEDED);
        }

        short sortOrder = (short) existing.size();
        String imageKey =
                String.format(
                        "star-images/%d/%d/%s.%s",
                        command.userId(),
                        command.starRecordId(),
                        UUID.randomUUID(),
                        toExtension(command.mimeType()));

        PresignedUrlResult presigned =
                presignedUrlGeneratorPort.generate(imageKey, command.mimeType());

        StarImage saved =
                starImageWritePort.save(
                        StarImage.create(
                                record,
                                imageKey,
                                presigned.imageUrl(),
                                command.mimeType(),
                                command.sizeBytes(),
                                sortOrder));

        return new UploadStarImageResult(
                saved.getId(), presigned.presignedUrl(), presigned.imageUrl());
    }

    private String toExtension(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }
}
