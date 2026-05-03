package com.groute.groute_server.record.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groute.groute_server.record.domain.StarTag;

public interface StarTagRepository extends JpaRepository<StarTag, Long> {

    /**
     * 특정 STAR 기록의 모든 태그를 조회한다.
     *
     * <p>REC-007 결과 조회 시 사용. AI 태깅 성공 후 primary_category 1개 + detail_tag 1~3개 row가 존재한다.
     *
     * @param starRecordId 조회할 STAR 기록 ID
     * @return 태그 목록
     */
    List<StarTag> findAllByStarRecordId(Long starRecordId);
}
