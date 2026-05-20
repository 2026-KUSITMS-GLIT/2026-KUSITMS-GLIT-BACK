package com.groute.groute_server.record.application.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.record.application.port.in.star.ConfirmStarImageCommand;
import com.groute.groute_server.record.application.port.in.star.ConfirmStarImageUseCase;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageWritePort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.StarImage;
import com.groute.groute_server.record.domain.StarRecord;

import lombok.RequiredArgsConstructor;

/** S3 업로드 완료 후 STAR 이미지 DB 저장 서비스 (POST /api/star-records/{id}/images/confirm). */
@Service
@RequiredArgsConstructor
@Transactional
public class ConfirmStarImageService implements ConfirmStarImageUseCase {

    private static final int MAX_IMAGES_PER_STAR = 2;

    private final StarRecordRepositoryPort starRecordRepositoryPort;
    private final StarImageQueryPort starImageQueryPort;
    private final StarImageWritePort starImageWritePort;
    private final PresignedUrlGeneratorPort presignedUrlGeneratorPort;

    @Override
    public void confirm(ConfirmStarImageCommand command) {
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

        List<String> imageKeys = command.imageKeys();
        if (new HashSet<>(imageKeys).size() != imageKeys.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        List<StarImage> existing =
                starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(command.starRecordId());
        if (existing.size() + imageKeys.size() > MAX_IMAGES_PER_STAR) {
            throw new BusinessException(ErrorCode.STAR_IMAGE_LIMIT_EXCEEDED);
        }

        String expectedPrefix =
                String.format("star-images/%d/%d/", command.userId(), command.starRecordId());
        for (String imageKey : imageKeys) {
            if (!imageKey.startsWith(expectedPrefix)) {
                throw new BusinessException(ErrorCode.STAR_FORBIDDEN);
            }
        }

        short sortOrder = (short) existing.size();
        for (String imageKey : imageKeys) {
            String imageUrl = presignedUrlGeneratorPort.toImageUrl(imageKey);
            starImageWritePort.save(StarImage.create(record, imageKey, imageUrl, sortOrder++));
        }
    }
}
