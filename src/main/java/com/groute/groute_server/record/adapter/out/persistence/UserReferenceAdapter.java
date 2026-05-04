package com.groute.groute_server.record.adapter.out.persistence;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.user.UserReferencePort;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * {@link UserReferencePort}의 JPA 어댑터.
 *
 * <p>신규 record 엔티티의 user FK를 채울 때 사용한다. user를 실제로 조회하지 않고 ID 기반 참조만 반환해 불필요한 SELECT를 피한다.
 *
 * <p>존재하지 않는 userId일 경우 저장 시점에 FK 제약으로 실패한다.
 */
@Component
@RequiredArgsConstructor
class UserReferenceAdapter implements UserReferencePort {

    private final EntityManager entityManager;

    @Override
    public User getReferenceById(Long userId) {
        return entityManager.getReference(User.class, userId);
    }
}
