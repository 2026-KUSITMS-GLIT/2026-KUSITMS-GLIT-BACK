package com.groute.groute_server.record.application.port.out;

import java.util.Optional;

import com.groute.groute_server.record.domain.StarRecord;

/**
 * STAR 기록 저장소 포트.
 *
 * <p>서비스가 DB를 직접 알지 않도록 인터페이스로 분리한다. 실제 구현은 {@code StarRecordPersistenceAdapter}가 담당한다.
 */
public interface StarRecordPort {

    /**
     * STAR 기록 ID로 조회한다.
     *
     * <p>논리 삭제된 레코드는 제외한다.
     *
     * @param starRecordId 조회할 STAR 기록 ID
     * @return STAR 기록 (없으면 empty)
     */
    Optional<StarRecord> findById(Long starRecordId);
}
