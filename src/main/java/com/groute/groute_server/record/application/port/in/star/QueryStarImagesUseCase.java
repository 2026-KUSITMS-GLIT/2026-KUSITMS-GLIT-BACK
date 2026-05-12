package com.groute.groute_server.record.application.port.in.star;

import java.util.List;

/** STAR 이미지 목록 조회 유스케이스. */
public interface QueryStarImagesUseCase {

    List<QueryStarImagesResult> query(Long userId, Long starRecordId);
}
