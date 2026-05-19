package com.groute.groute_server.record.adapter.out.persistence;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Component;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.out.user.UserStreakPort;
import com.groute.groute_server.user.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * {@link UserStreakPort}의 JPA 어댑터.
 *
 * <p>{@link EntityManager#find}로 user를 가져온 뒤 도메인 메서드(연속 기록 갱신)를 호출하기만 한다. 변경 내용은 JPA dirty
 * checking이 트랜잭션 커밋 시점에 자동으로 DB에 반영하므로, 호출자는 같은 {@code @Transactional} 안에서 본 어댑터를 호출해야 한다.
 *
 * <p>존재하지 않는 userId가 들어오면 {@link ErrorCode#USER_NOT_FOUND}로 즉시 실패한다. 작성 use case가 인증된 사용자 ID를 그대로
 * 넘기는 흐름이라 실 운영에서는 거의 발생하지 않지만, 계약상 명확히 해 둔다.
 */
@Component
@RequiredArgsConstructor
class UserStreakAdapter implements UserStreakPort {

    private final EntityManager entityManager;

    @Override
    public void recordOnDate(Long userId, LocalDate kstDate) {
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.recordOnDate(kstDate);
    }
}
