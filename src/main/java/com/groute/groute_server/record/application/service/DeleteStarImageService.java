package com.groute.groute_server.record.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.common.transaction.AfterCommitExecutor;
import com.groute.groute_server.record.application.port.in.star.DeleteStarImageUseCase;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageWritePort;
import com.groute.groute_server.record.domain.StarImage;
import com.groute.groute_server.record.domain.StarRecord;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteStarImageService implements DeleteStarImageUseCase {

    private final StarImageQueryPort starImageQueryPort;
    private final StarImageWritePort starImageWritePort;
    private final PresignedUrlGeneratorPort presignedUrlGeneratorPort;
    private final AfterCommitExecutor afterCommitExecutor;

    @Override
    public void delete(Long userId, Long starRecordId, Long imageId) {
        StarImage image =
                starImageQueryPort
                        .findById(imageId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STAR_IMAGE_NOT_FOUND));

        StarRecord starRecord = image.getStarRecord();
        if (!starRecord.getId().equals(starRecordId)) {
            throw new BusinessException(ErrorCode.STAR_IMAGE_NOT_FOUND);
        }
        if (!starRecord.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.STAR_FORBIDDEN);
        }
        if (starRecord.isWriteLocked()) {
            throw new BusinessException(ErrorCode.STAR_WRITE_LOCKED);
        }

        starImageWritePort.deleteById(imageId);
        String key = image.getImageKey();
        afterCommitExecutor.execute(() -> presignedUrlGeneratorPort.deleteObject(key));
    }
}
