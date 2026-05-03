package com.groute.groute_server.record.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.out.UserPort;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** record 도메인에서 사용하는 사용자 조회 어댑터. */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPort {

    private final UserRepository userRepository;

    @Override
    public User findById(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}