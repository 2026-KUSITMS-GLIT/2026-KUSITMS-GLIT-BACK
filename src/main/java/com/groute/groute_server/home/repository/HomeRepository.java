package com.groute.groute_server.home.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.home.dto.CompetencyCount;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.StarRecordStatus;

public interface HomeRepository extends JpaRepository<StarRecord, Long> {

    /** TAGGED 완료된 STAR 기록의 역량 카테고리별 건수. selectedCompetency가 NULL인 스크럼은 제외. */
    @Query(
            "SELECT new com.groute.groute_server.home.dto.CompetencyCount("
                    + "s.selectedCompetency, COUNT(sr)) "
                    + "FROM StarRecord sr JOIN sr.scrum s "
                    + "WHERE sr.user.id = :userId "
                    + "AND sr.status = :tagged "
                    + "AND sr.isDeleted = false "
                    + "AND s.selectedCompetency IS NOT NULL "
                    + "GROUP BY s.selectedCompetency")
    List<CompetencyCount> countCompletedByCompetency(
            @Param("userId") Long userId, @Param("tagged") StarRecordStatus tagged);
}
