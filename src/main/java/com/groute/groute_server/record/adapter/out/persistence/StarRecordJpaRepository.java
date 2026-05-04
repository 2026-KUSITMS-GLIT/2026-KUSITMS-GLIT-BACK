package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.StarRecord;

/**
 * 심화 STAR 기록(StarRecord) JPA 레포지토리.
 *
 * <p>스크럼 sync 시 삭제 대상 Scrum의 STAR 기록을 cascade soft-delete 하기 위해 사용한다.
 */
public interface StarRecordJpaRepository extends JpaRepository<StarRecord, Long> {

    /** 해당 스크럼에 살아있는 STAR 기록이 있는지. */
    @Query(
            "SELECT (count(sr) > 0) FROM StarRecord sr "
                    + "WHERE sr.scrum.id = :scrumId AND sr.isDeleted = false")
    boolean existsByScrumId(@Param("scrumId") Long scrumId);

    /** Scrum cascade soft-delete. 동일 트랜잭션에서 Scrum 삭제와 함께 호출. */
    @Modifying
    @Query(
            "UPDATE StarRecord sr "
                    + "SET sr.isDeleted = true, sr.deletedAt = CURRENT_TIMESTAMP "
                    + "WHERE sr.scrum.id IN :scrumIds AND sr.isDeleted = false")
    int deleteAllByScrumIdIn(@Param("scrumIds") Collection<Long> scrumIds);
}
