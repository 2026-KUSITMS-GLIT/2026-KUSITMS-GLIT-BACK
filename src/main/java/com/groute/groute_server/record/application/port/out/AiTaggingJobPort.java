package com.groute.groute_server.record.application.port.out;

import java.util.Optional;

import com.groute.groute_server.record.domain.AiTaggingJob;
import com.groute.groute_server.record.domain.StarRecord;

/**
 * AI 태깅 잡 저장소 포트.
 *
 * <p>서비스가 DB를 직접 알지 않도록 인터페이스로 분리한다. 실제 구현은 {@code AiTaggingJobPersistenceAdapter}가 담당한다.
 */
public interface AiTaggingJobPort {

    /**
     * 특정 STAR 기록의 가장 최근 잡을 조회한다.
     *
     * <p>생성일 기준 내림차순으로 1건만 반환한다. 잡이 없으면 Optional.empty() 반환.
     *
     * @param starRecordId 조회할 STAR 기록 ID
     * @return 가장 최근 잡 (없으면 empty)
     */
    Optional<AiTaggingJob> findLatestByStarRecordId(Long starRecordId);

    /**
     * 새 잡을 저장한다.
     *
     * @param starRecord 잡을 생성할 STAR 기록
     * @return 저장된 잡
     */
    AiTaggingJob save(StarRecord starRecord);
}
