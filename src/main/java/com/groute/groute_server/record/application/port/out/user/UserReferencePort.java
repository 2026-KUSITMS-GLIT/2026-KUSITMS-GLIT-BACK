package com.groute.groute_server.record.application.port.out.user;

import com.groute.groute_server.user.entity.User;

/**
 * 신규 record 엔티티의 user FK 참조 획득용 포트.
 *
 * <p>JPA {@code getReference}로 프록시만 반환하여 user 도메인을 실제 로딩하지 않고 record 도메인의 cross-domain 의존을 격리한다.
 */
public interface UserReferencePort {

    User getReferenceById(Long userId);
}
