package com.groute.groute_server.report.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.report.domain.Report;

/**
 * 리포트(Report) JPA 레포지토리.
 *
 * <p>현재는 회원 탈퇴 hard delete 배치(MYP-005) 진입점만 제공한다. 리포트 발행/조회 API는 별도 추가 예정.
 */
interface ReportJpaRepository extends JpaRepository<Report, Long> {

    /**
     * 해당 사용자가 발행한 모든 Report 물리 삭제(MYP-005 hard delete 배치).
     *
     * <p>Report는 다른 도메인에서 참조하지 않는 leaf 테이블이라 호출 순서 제약 없음. 복구 불가.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query("DELETE FROM Report r WHERE r.user.id = :userId")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
