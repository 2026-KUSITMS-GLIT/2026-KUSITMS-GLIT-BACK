package com.groute.groute_server.report.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.Scrum;

/**
 * 리포트 도메인에서 사용하는 스크럼 조회 전용 JPA 레포지토리.
 *
 * <p>선택된 심화기록 날짜에 속한 스크럼을 수집하여 AI 인풋을 구성하는 데 사용한다.
 */
public interface ScrumForReportJpaRepository extends JpaRepository<Scrum, Long> {

    /**
     * 선택된 심화기록 날짜에 속한 유저의 모든 스크럼을 반환한다.
     *
     * <p>AI 인풋 구성 시 선택된 심화기록과 함께 해당 날짜의 스크럼을 자동 포함한다.
     */
    @Query(
            "SELECT s FROM Scrum s "
                    + "WHERE s.user.id = :userId "
                    + "AND s.scrumDate IN ("
                    + "  SELECT sr.scrum.scrumDate FROM StarRecord sr "
                    + "  WHERE sr.id IN :starRecordIds "
                    + "  AND sr.user.id = :userId "
                    + "  AND sr.isDeleted = false"
                    + ") "
                    + "AND s.isDeleted = false")
    List<Scrum> findScrumsByStarRecordIds(
            @Param("userId") Long userId, @Param("starRecordIds") List<Long> starRecordIds);
}
