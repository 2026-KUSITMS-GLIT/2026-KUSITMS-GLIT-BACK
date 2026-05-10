package com.groute.groute_server.record.application.port.out.scrum;

import java.util.Collection;
import java.util.List;

import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;

/** Scrum 쓰기 포트. */
public interface ScrumWritePort {

    /** 신규 Scrum 일괄 저장. */
    List<Scrum> saveAll(Collection<Scrum> scrums);

    /** 본문 변경. */
    void updateContent(Long scrumId, String content);

    /** soft-delete (is_deleted=true). cascade는 호출자가 별도 포트로 처리. */
    void softDeleteAllByIdIn(Collection<Long> ids);

    /** STAR 단독 삭제 시 Scrum.hasStar 플래그를 false로 동기화. */
    void clearHasStar(Long scrumId);

    /** STAR 시작 전 5대 역량 선택. hasStar=false인 경우에만 업데이트되며 성공 여부 반환. */
    boolean updateCompetency(Long scrumId, CompetencyCategory competency);
}
