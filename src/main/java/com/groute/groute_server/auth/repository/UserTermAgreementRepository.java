package com.groute.groute_server.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.auth.entity.UserTermAgreement;

/**
 * 사용자 약관 동의 이력(UserTermAgreement) 저장소.
 *
 * <p>현재는 회원 탈퇴 hard delete 배치(MYP-005) 진입점만 노출한다. 약관 동의 조회·기록 API는 별도 추가 예정.
 */
public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, Long> {

    /**
     * 회원 탈퇴 hard delete 배치(MYP-005) 진입. 해당 사용자의 모든 약관 동의 이력 row 물리 삭제.
     *
     * <p>법적 보존 의무가 있는 동의 이력의 보존 정책은 본 PR 범위 외 — 별도 정책 결정 시 본 메서드를 보존 destination으로 우회시키거나 호출자에서 분기.
     * 현재는 모두 즉시 물리 삭제. 복구 불가.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query("DELETE FROM UserTermAgreement a WHERE a.user.id = :userId")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
