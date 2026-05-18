package com.groute.groute_server.report.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.StarTag;

/**
 * 리포트 도메인에서 사용하는 역량 태그 조회 전용 JPA 레포지토리.
 *
 * <p>AI 리포트 요청 body 구성 시 심화기록별 detailTags 수집에 사용한다.
 */
public interface StarTagForReportJpaRepository extends JpaRepository<StarTag, Long> {

    /** 심화기록 ID 목록에 해당하는 태그를 한 번에 조회한다. */
    @Query(
            "SELECT t FROM StarTag t "
                    + "WHERE t.starRecord.id IN :starRecordIds "
                    + "ORDER BY t.starRecord.id ASC, t.id ASC")
    List<StarTag> findAllByStarRecordIds(@Param("starRecordIds") List<Long> starRecordIds);
}
