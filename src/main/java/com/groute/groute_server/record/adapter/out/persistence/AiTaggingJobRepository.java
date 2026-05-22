package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.AiTaggingJob;

public interface AiTaggingJobRepository extends JpaRepository<AiTaggingJob, Long> {

    /**
     * 특정 STAR 기록의 가장 최근 잡을 조회한다.
     *
     * <p>REC-005 트리거 시 기존 잡 상태 확인, REC-006 상태 폴링에 사용한다. 생성일 기준 내림차순으로 1건만 반환한다.
     *
     * @param starRecordId 조회할 STAR 기록 ID
     * @return 가장 최근 잡 (없으면 empty)
     */
    Optional<AiTaggingJob> findTopByStarRecordIdOrderByCreatedAtDesc(Long starRecordId);

    /**
     * 해당 사용자가 소유한 모든 AiTaggingJob 물리 삭제(MYP-005 hard delete 배치).
     *
     * <p>AiTaggingJob엔 user_id 컬럼이 없어 부모 StarRecord를 거쳐 subquery로 매핑한다. 부모 StarRecord 삭제 전에 호출되어야
     * FK 위반을 피한다. 복구 불가.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query(
            "DELETE FROM AiTaggingJob j "
                    + "WHERE j.starRecord.id IN ("
                    + "  SELECT sr.id FROM StarRecord sr WHERE sr.user.id = :userId)")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
