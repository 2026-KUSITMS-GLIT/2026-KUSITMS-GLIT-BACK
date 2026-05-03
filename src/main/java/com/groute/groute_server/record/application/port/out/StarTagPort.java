package com.groute.groute_server.record.application.port.out;

import java.util.List;

import com.groute.groute_server.record.domain.StarTag;

/**
 * AI 태깅 결과 태그 저장소 포트.
 *
 * <p>서비스가 DB를 직접 알지 않도록 인터페이스로 분리한다.
 * 실제 구현은 {@code StarTagPersistenceAdapter}가 담당한다.
 */
public interface StarTagPort {

    /**
     * 특정 STAR 기록의 모든 태그를 조회한다.
     *
     * <p>REC-007 결과 조회 시 사용. AI 태깅 성공 후 1개 이상 존재한다.
     *
     * @param starRecordId 조회할 STAR 기록 ID
     * @return 태그 목록 (primary_category 1개 + detail_tag 1~3개 row)
     */
    List<StarTag> findAllByStarRecordId(Long starRecordId);
}
