package com.groute.groute_server.record.application.port.out.scrumtitle;

import java.util.Collection;
import java.util.List;

import com.groute.groute_server.record.domain.ScrumTitle;

/** ScrumTitle 영속성 포트. */
public interface ScrumTitleRepositoryPort {

    /** 신규 ScrumTitle 일괄 저장. 저장 순서 보장(입력 순 = 반환 순). */
    List<ScrumTitle> saveAll(List<ScrumTitle> titles);

    /** 요청 titleId 중 본인 소유인 것만 반환. 결과 크기로 미존재/타인 소유 판별. */
    List<ScrumTitle> findAllByIdInAndUserId(Collection<Long> ids, Long userId);

    /** 비정규화 카운터 동기화. increment는 음수 가능 (Scrum 신규 +1, 삭제 -1). */
    void applyScrumCountIncrement(Long titleId, int increment);

    /** 지정 title 전체를 PENDING → COMMITTED 로 전환. 모든 STAR 완료 시 호출. */
    int commitAllByIds(Collection<Long> titleIds);
}
