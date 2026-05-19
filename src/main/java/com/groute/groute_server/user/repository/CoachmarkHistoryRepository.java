package com.groute.groute_server.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.user.entity.CoachmarkHistory;

/**
 * 코치마크 노출 이력(CoachmarkHistory) 저장소.
 *
 * <p>현재는 회원 탈퇴 hard delete 배치(MYP-005) 진입점만 노출한다. 노출 이력 조회·기록 API는 별도 추가 예정.
 */
public interface CoachmarkHistoryRepository extends JpaRepository<CoachmarkHistory, Long> {

    /**
     * 회원 탈퇴 hard delete 배치(MYP-005) 진입. 해당 사용자의 모든 코치마크 이력 row 물리 삭제.
     *
     * <p>다른 도메인에서 참조하지 않는 leaf 테이블이라 호출 순서 제약 없음. 복구 불가.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query("DELETE FROM CoachmarkHistory c WHERE c.user.id = :userId")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
