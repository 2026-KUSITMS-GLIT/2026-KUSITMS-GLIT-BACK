package com.groute.groute_server.record.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.star.StarTagQueryPort;
import com.groute.groute_server.record.domain.StarTag;

import lombok.RequiredArgsConstructor;

/**
 * {@link StarTagQueryPort}의 JPA 어댑터.
 *
 * <p>심화기록 상세 응답의 primary 역량 + detail 해시태그 목록을 제공한다.
 */
@Component
@RequiredArgsConstructor
class StarTagPersistenceAdapter implements StarTagQueryPort {

    private final StarTagJpaRepository jpaRepository;

    @Override
    public List<StarTag> findAllByStarRecordId(Long starRecordId) {
        return jpaRepository.findAllByStarRecordId(starRecordId);
    }
}
