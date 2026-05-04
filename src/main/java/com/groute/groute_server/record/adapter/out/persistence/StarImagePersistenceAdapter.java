package com.groute.groute_server.record.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.star.StarImageQueryPort;
import com.groute.groute_server.record.domain.StarImage;

import lombok.RequiredArgsConstructor;

/**
 * {@link StarImageQueryPort}의 JPA 어댑터.
 *
 * <p>심화기록 상세 응답의 이미지 목록을 sortOrder 오름차순으로 제공한다.
 */
@Component
@RequiredArgsConstructor
class StarImagePersistenceAdapter implements StarImageQueryPort {

    private final StarImageJpaRepository jpaRepository;

    @Override
    public List<StarImage> findAllByStarRecordIdOrderBySortOrder(Long starRecordId) {
        return jpaRepository.findAllByStarRecordIdOrderBySortOrderAsc(starRecordId);
    }
}
