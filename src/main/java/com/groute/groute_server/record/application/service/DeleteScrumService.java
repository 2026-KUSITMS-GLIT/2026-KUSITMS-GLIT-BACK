package com.groute.groute_server.record.application.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.config.CacheConfig;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.transaction.AfterCommitExecutor;
import com.groute.groute_server.record.application.port.in.scrum.DeleteScrumCommand;
import com.groute.groute_server.record.application.port.in.scrum.DeleteScrumUseCase;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordCascadePort;
import com.groute.groute_server.record.domain.Scrum;

import lombok.RequiredArgsConstructor;

/**
 * 스크럼 단일 삭제 서비스.
 *
 * <p>Scrum soft-delete + 연결된 STAR·첨부 이미지 cascade 정리 + ScrumTitle.scrumCount 1 감소. 14일 편집 윈도우와
 * hasStar 거부 정책은 적용하지 않으며, STAR가 있는 스크럼이라도 그대로 cascade 삭제한다. 호출 순서는 {@link ScrumSyncService} 의 삭제
 * 분기와 일치시켜 트랜잭션 불변식을 동일하게 유지한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteScrumService implements DeleteScrumUseCase {

    private final ScrumQueryPort scrumQueryPort;
    private final ScrumWritePort scrumWritePort;
    private final ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    private final StarRecordCascadePort starRecordCascadePort;
    private final StarImageCascadeCleaner starImageCascadeCleaner;
    private final CacheManager cacheManager;
    private final AfterCommitExecutor afterCommitExecutor;

    @Override
    public void deleteScrum(DeleteScrumCommand command) {
        Set<Long> scrumIds = Set.of(command.scrumId());

        List<Scrum> owned = scrumQueryPort.findAllByIdInAndUserId(scrumIds, command.userId());
        if (owned.isEmpty()) {
            throw new BusinessException(ErrorCode.SCRUM_NOT_FOUND);
        }
        Long titleId = owned.get(0).getTitle().getId();

        starImageCascadeCleaner.cleanupByScrumIds(scrumIds);
        scrumWritePort.softDeleteAllByIdIn(scrumIds);
        starRecordCascadePort.cascadeDeleteByScrumIdIn(scrumIds);
        scrumTitleRepositoryPort.applyScrumCountIncrement(titleId, -1);

        // calendar:monthly 캐시 무효화 (커밋 후)
        final Long userId = command.userId();
        final YearMonth month = YearMonth.from(owned.get(0).getScrumDate());
        afterCommitExecutor.execute(
                () -> {
                    Cache cache = cacheManager.getCache(CacheConfig.CACHE_CALENDAR_MONTHLY);
                    if (cache != null) {
                        cache.evict(userId + ":" + month);
                    }
                });
    }
}
