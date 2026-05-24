package com.groute.groute_server.record.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.star.ScrumStarTagProjection;
import com.groute.groute_server.record.application.port.out.star.StarTagQueryPort;
import com.groute.groute_server.record.application.port.out.star.StarTagSavePort;
import com.groute.groute_server.record.domain.StarTag;

import lombok.RequiredArgsConstructor;

/**
 * {@link StarTagQueryPort}의 JPA 어댑터.
 *
 * <p>심화기록 상세 응답의 primary 역량 + detail 해시태그 목록을 제공한다.
 */
@Component
@RequiredArgsConstructor
class StarTagPersistenceAdapter implements StarTagQueryPort, StarTagSavePort {

    private final StarTagJpaRepository jpaRepository;

    @Override
    public List<StarTag> findAllByStarRecordId(Long starRecordId) {
        return jpaRepository.findAllByStarRecordId(starRecordId);
    }

    @Override
    public List<ScrumStarTagProjection> findCompletedTagsByScrumIds(
            Long userId, List<Long> scrumIds) {
        if (scrumIds == null || scrumIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findCompletedTagsByScrumIds(userId, scrumIds);
    }

    @Override
    public void saveAll(List<StarTag> tags) {
        jpaRepository.saveAll(tags);
    }
}
