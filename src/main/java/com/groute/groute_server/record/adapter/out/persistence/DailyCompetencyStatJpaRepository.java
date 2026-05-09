package com.groute.groute_server.record.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.DailyCompetencyStat;

/**
 * 일자별 역량 통계(DailyCompetencyStat) JPA 레포지토리.
 *
 * <p>홈 잔디·레이더 차트 성능 최적화용 비정규화 테이블의 영속성 진입점. 현재는 회원 탈퇴 hard delete 배치에서만 사용되며, 통계 upsert 진입점은 별도 추가
 * 예정.
 */
interface DailyCompetencyStatJpaRepository extends JpaRepository<DailyCompetencyStat, Long> {

    /**
     * 해당 사용자가 소유한 모든 DailyCompetencyStat 물리 삭제(MYP-005 hard delete 배치).
     *
     * <p>다른 도메인에 대한 FK가 없는 독립 테이블이라 호출 순서 제약 없음. 복구 불가.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query("DELETE FROM DailyCompetencyStat d WHERE d.user.id = :userId")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
