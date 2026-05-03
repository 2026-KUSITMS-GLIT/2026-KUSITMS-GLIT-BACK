package com.groute.groute_server.record.application.port.out;

import com.groute.groute_server.user.entity.User;

/** record 도메인에서 필요한 사용자 조회 포트. */
public interface UserPort {

    User findById(Long userId);
}
