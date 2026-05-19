package com.groute.groute_server.report.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.report.application.port.out.LoadUserPort;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * {@link LoadUserPort}의 JPA 어댑터.
 *
 * <p>리포트 도메인에서 유저 조회 시 UserRepository를 직접 의존하지 않도록 포트로 격리한다.
 */
@Component
@RequiredArgsConstructor
class UserForReportPersistenceAdapter implements LoadUserPort {

    private final UserRepository userRepository;

    @Override
    public User findUserById(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
