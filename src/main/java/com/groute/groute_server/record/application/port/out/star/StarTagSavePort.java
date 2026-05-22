package com.groute.groute_server.record.application.port.out.star;

import java.util.List;

import com.groute.groute_server.record.domain.StarTag;

/** StarTag 저장 포트. AI 태깅 완료 시 결과를 star_tags 테이블에 저장한다. */
public interface StarTagSavePort {

    /**
     * StarTag 목록을 저장한다.
     *
     * @param tags 저장할 StarTag 목록
     */
    void saveAll(List<StarTag> tags);
}
