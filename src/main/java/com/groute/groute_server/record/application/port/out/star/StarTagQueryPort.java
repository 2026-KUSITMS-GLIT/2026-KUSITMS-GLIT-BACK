package com.groute.groute_server.record.application.port.out.star;

import java.util.List;

import com.groute.groute_server.record.domain.StarTag;

/** StarTag 조회 포트. 심화기록 상세 응답의 primary 역량 + detail 해시태그 추출에 사용. */
public interface StarTagQueryPort {

    List<StarTag> findAllByStarRecordId(Long starRecordId);

    /**
     * 지정 scrumId 집합 중 완료된 STAR의 태그 row 목록.
     *
     * <p>일자별 스크럼 조회에서 카드 단위 primaryCategory 배열과 일자 단위 detailTag 집합을 동시에 산출하기 위해 사용한다. 호출 측에서 본인
     * scrumIds만 전달한다고 신뢰하지 않고 저장소 단에서도 {@code userId}로 한 번 더 강제(defense-in-depth). soft-delete된
     * starRecord는 제외. 빈 입력엔 빈 리스트.
     */
    List<ScrumStarTagProjection> findCompletedTagsByScrumIds(Long userId, List<Long> scrumIds);
}
