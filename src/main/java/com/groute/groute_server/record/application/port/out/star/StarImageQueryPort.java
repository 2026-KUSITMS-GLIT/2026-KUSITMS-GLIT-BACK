package com.groute.groute_server.record.application.port.out.star;

import java.util.List;

import com.groute.groute_server.record.domain.StarImage;

/** StarImage 조회 포트. 심화기록 상세 응답의 이미지 목록(sortOrder 오름차순) 추출에 사용. */
public interface StarImageQueryPort {

    List<StarImage> findAllByStarRecordIdOrderBySortOrder(Long starRecordId);
}
