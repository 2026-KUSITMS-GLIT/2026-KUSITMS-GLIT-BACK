package com.groute.groute_server.record.application.port.out.scrum;

import java.util.Collection;
import java.util.List;

import com.groute.groute_server.record.domain.Scrum;

/** Scrum 쓰기 포트. */
public interface ScrumWritePort {

    /** 신규 Scrum 일괄 저장. */
    List<Scrum> saveAll(Collection<Scrum> scrums);

    /** 본문 변경. */
    void updateContent(Long scrumId, String content);

    /** soft-delete (is_deleted=true). cascade는 호출자가 별도 포트로 처리. */
    void softDeleteAllByIdIn(Collection<Long> ids);
}
