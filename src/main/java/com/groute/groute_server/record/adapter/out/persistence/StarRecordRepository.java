package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groute.groute_server.record.domain.StarRecord;

public interface StarRecordRepository extends JpaRepository<StarRecord, Long> {

    /**
     * STAR 기록 ID로 조회한다. 논리 삭제된 레코드는 제외한다.
     *
     * @param id 조회할 STAR 기록 ID
     * @return STAR 기록 (없으면 empty)
     */
    Optional<StarRecord> findByIdAndIsDeletedFalse(Long id);
}
