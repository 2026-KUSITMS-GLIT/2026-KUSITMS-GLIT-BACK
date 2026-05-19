package com.groute.groute_server.record.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.groute.groute_server.common.storage.PresignedUrlGeneratorPort;
import com.groute.groute_server.record.application.port.in.RecordAccountHardDeleteUseCase;
import com.groute.groute_server.record.application.port.out.RecordHardDeletePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link RecordAccountHardDeleteUseCase} 구현(MYP-005).
 *
 * <p>외부 도메인이 record 내부 port/out에 직접 결합하지 않도록 use case로 한 단계 추상화한다. 외부 스토리지 삭제는 키 수집 → S3
 * best-effort 삭제 → DB row hard delete 순서로 진행된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordAccountHardDeleteService implements RecordAccountHardDeleteUseCase {

    private final RecordHardDeletePort recordHardDeletePort;
    private final PresignedUrlGeneratorPort presignedUrlGeneratorPort;

    @Override
    public void purgeExternalStorage(Long userId) {
        List<String> keys = recordHardDeletePort.findStarImageKeysByUserId(userId);
        if (keys.isEmpty()) {
            return;
        }
        int failed = 0;
        for (String key : keys) {
            try {
                presignedUrlGeneratorPort.deleteObject(key);
            } catch (RuntimeException e) {
                failed++;
                log.warn("S3 오브젝트 삭제 실패 (userId={}, key={})", userId, key, e);
            }
        }
        log.info(
                "StarImage 외부 스토리지 정리 완료 (userId={}, total={}, failed={})",
                userId,
                keys.size(),
                failed);
    }

    @Override
    public void purgeDb(Long userId) {
        recordHardDeletePort.hardDeleteAllByUserId(userId);
    }
}
