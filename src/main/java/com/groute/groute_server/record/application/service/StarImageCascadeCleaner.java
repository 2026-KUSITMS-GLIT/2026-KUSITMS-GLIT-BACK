package com.groute.groute_server.record.application.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.common.transaction.AfterCommitExecutor;
import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarImageWritePort;
import com.groute.groute_server.record.domain.StarImage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * StarImage DB 삭제 + 커밋 후 S3 삭제를 묶어 처리하는 공통 유틸리티.
 *
 * <p>DB 삭제 → afterCommit S3 삭제 순서 불변식을 한 곳에서 관리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StarImageCascadeCleaner {

    private final StarImageQueryPort starImageQueryPort;
    private final StarImageWritePort starImageWritePort;
    private final PresignedUrlGeneratorPort presignedUrlGeneratorPort;
    private final AfterCommitExecutor afterCommitExecutor;

    public void cleanupByScrumIds(Collection<Long> scrumIds) {
        if (scrumIds.isEmpty()) return;
        cleanup(starImageQueryPort.findAllByScrumIdIn(scrumIds));
    }

    public void cleanupByStarRecordId(Long starRecordId) {
        cleanup(starImageQueryPort.findAllByStarRecordIdOrderBySortOrder(starRecordId));
    }

    private void cleanup(List<StarImage> images) {
        if (images.isEmpty()) return;
        starImageWritePort.deleteAll(images);
        List<String> keys =
                images.stream()
                        .map(StarImage::getImageKey)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        afterCommitExecutor.execute(
                () -> {
                    for (String key : keys) {
                        try {
                            presignedUrlGeneratorPort.deleteObject(key);
                        } catch (RuntimeException e) {
                            log.warn("S3 오브젝트 삭제 실패 — key={}", key, e);
                        }
                    }
                });
    }
}
