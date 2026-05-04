package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.star.StarRecordCascadePort;

import lombok.RequiredArgsConstructor;

/**
 * {@link StarRecordCascadePort}의 JPA 어댑터.
 *
 * <p>Scrum 삭제 시 연결된 STAR 기록을 같은 트랜잭션에서 함께 soft-delete 한다.
 */
@Component
@RequiredArgsConstructor
class StarRecordCascadeAdapter implements StarRecordCascadePort {

    private final StarRecordJpaRepository jpaRepository;

    @Override
    public void cascadeDeleteByScrumIdIn(Collection<Long> scrumIds) {
        if (scrumIds.isEmpty()) {
            return;
        }
        jpaRepository.deleteAllByScrumIdIn(scrumIds);
    }
}
