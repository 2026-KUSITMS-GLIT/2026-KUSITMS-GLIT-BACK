package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.domain.ScrumTitle;

import lombok.RequiredArgsConstructor;

/**
 * {@link ScrumTitleRepositoryPort}의 JPA 어댑터.
 *
 * <p>요청 titleId의 일괄 소유권 검증과 비정규화 카운터 갱신을 담당한다.
 */
@Component
@RequiredArgsConstructor
class ScrumTitlePersistenceAdapter implements ScrumTitleRepositoryPort {

    private final ScrumTitleJpaRepository jpaRepository;

    @Override
    public List<ScrumTitle> findAllByIdInAndUserId(Collection<Long> ids, Long userId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllByIdInAndUserId(ids, userId);
    }

    @Override
    public void applyScrumCountIncrement(Long titleId, int increment) {
        if (increment == 0) {
            return;
        }
        jpaRepository.applyScrumCountIncrement(titleId, increment);
    }
}
