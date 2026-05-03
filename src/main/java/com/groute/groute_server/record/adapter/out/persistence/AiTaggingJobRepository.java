package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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
    @Query(
            "SELECT j FROM AiTaggingJob j WHERE j.starRecord.id = :starRecordId ORDER BY j.createdAt DESC LIMIT 1")
    Optional<AiTaggingJob> findLatestByStarRecordId(@Param("starRecordId") Long starRecordId);
}
