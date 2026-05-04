package com.groute.groute_server.record.application.port.out.star;

import java.util.List;

import com.groute.groute_server.record.domain.StarTag;

/** StarTag 조회 포트. 심화기록 상세 응답의 primary 역량 + detail 해시태그 추출에 사용. */
public interface StarTagQueryPort {

    List<StarTag> findAllByStarRecordId(Long starRecordId);
}
