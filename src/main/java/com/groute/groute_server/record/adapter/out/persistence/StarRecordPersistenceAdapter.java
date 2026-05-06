package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.domain.StarRecord;

import lombok.RequiredArgsConstructor;

/**
 * {@link StarRecordRepositoryPort}의 JPA 어댑터.
 *
 * <p>심화기록 단건 상세 조회와 단건 soft-delete를 담당한다. 소유권 검증은 호출자 책임.
 */
@Component
@RequiredArgsConstructor
class StarRecordPersistenceAdapter implements StarRecordRepositoryPort {

    private final StarRecordJpaRepository jpaRepository;

    @Override
    public Optional<StarRecord> findByIdWithScrum(Long id) {
        return jpaRepository.findByIdWithScrum(id);
    }

    @Override
    public Optional<StarRecord> findById(Long starRecordId) {
        return jpaRepository.findByIdAndIsDeletedFalse(starRecordId);
    }

    @Override
    public void softDeleteById(Long id) {
        jpaRepository.softDeleteById(id);
    }
}
