package com.groute.groute_server.user.repository;

import java.time.OffsetDateTime;
import java.util.List;

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

    /**
     * 회원 탈퇴 hard delete 배치(MYP-005) 대상 사용자 ID 조회.
     *
     * <p>이미 탈퇴 처리되었고({@code is_deleted=true}), 예약된 grace 기간이 만료된({@code hard_delete_at <= now})
     * 사용자만 반환. 스케줄러가 본 결과를 받아 사용자별로 hard delete를 호출한다.
     *
     * @param now 비교 기준 시각 (UTC)
     * @return hard delete 대상 사용자 ID 목록
     */
    @Query("SELECT u.id FROM User u WHERE u.isDeleted = true AND u.hardDeleteAt <= :now")
    List<Long> findExpiredHardDeleteUserIds(@Param("now") OffsetDateTime now);
}
