package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordWritePort;
import com.groute.groute_server.record.domain.StarRecord;

import lombok.RequiredArgsConstructor;

/**
 * {@link StarRecordRepositoryPort}와 {@link StarRecordWritePort}의 JPA 어댑터.
 *
 * <p>같은 JPA 레포를 공유하므로 두 포트를 한 어댑터에서 함께 구현한다. 소유권 검증은 호출자 책임.
 */
@Component
@RequiredArgsConstructor
class StarRecordPersistenceAdapter implements StarRecordRepositoryPort, StarRecordWritePort {

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

    @Override
    public StarRecord save(StarRecord starRecord) {
        return jpaRepository.save(starRecord);
    }
}
