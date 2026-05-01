package com.groute.groute_server.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.enums.JobRole;
import com.groute.groute_server.user.enums.UserStatus;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * nickname IS NULL인 경우에만 온보딩 필드를 갱신한다.
     *
     * @return 갱신된 row 수 (0 = 이미 완료됐거나 존재하지 않는 유저, 1 = 성공)
     */
    @Modifying(clearAutomatically = true)
    @Query(
            "UPDATE User u SET u.nickname = :nickname, u.jobRole = :jobRole, u.userStatus = :userStatus"
                    + " WHERE u.id = :id AND u.nickname IS NULL")
    int completeOnboardingIfNotDone(
            @Param("id") Long id,
            @Param("nickname") String nickname,
            @Param("jobRole") JobRole jobRole,
            @Param("userStatus") UserStatus userStatus);
}
