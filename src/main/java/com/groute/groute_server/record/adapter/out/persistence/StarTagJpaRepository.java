package com.groute.groute_server.record.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.StarTag;

/**
 * STAR 역량 태그(StarTag) JPA 레포지토리.
 *
 * <p>심화기록 상세 응답의 primary 역량 + detail 해시태그 목록을 제공한다. StarTag는 BaseTimeEntity 기반이라 soft-delete 필터 없음
 * — 부모(StarRecord) 삭제 시 JOIN 필터로 자연 차단되는 구조.
 */
public interface StarTagJpaRepository extends JpaRepository<StarTag, Long> {

    @Query("SELECT t FROM StarTag t WHERE t.starRecord.id = :starRecordId ORDER BY t.id ASC")
    List<StarTag> findAllByStarRecordId(@Param("starRecordId") Long starRecordId);
}
