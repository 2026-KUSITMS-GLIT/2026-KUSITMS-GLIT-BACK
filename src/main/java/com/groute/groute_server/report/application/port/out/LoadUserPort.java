package com.groute.groute_server.report.application.port.out;

import com.groute.groute_server.user.entity.User;

/** 유저 조회 포트. 리포트 도메인이 user 도메인 DB에 접근하기 위한 out 포트. */
public interface LoadUserPort {

    /**
     * 유저 ID로 유저를 조회한다.
     *
     * @param userId 유저 PK
     * @return 유저
     */
    User findUserById(Long userId);
}
