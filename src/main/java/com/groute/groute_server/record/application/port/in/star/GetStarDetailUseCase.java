package com.groute.groute_server.record.application.port.in.star;

/** 심화기록 상세 조회 유스케이스 (CAL-003, GET /api/star-records/{starRecordId}). */
public interface GetStarDetailUseCase {

    StarDetailView getStarDetail(GetStarDetailQuery query);
}
