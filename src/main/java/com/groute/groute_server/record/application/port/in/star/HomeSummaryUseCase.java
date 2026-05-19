package com.groute.groute_server.record.application.port.in.star;

/** STAR 기록 후 홈 복귀 시 요약 정보 조회 유스케이스. */
public interface HomeSummaryUseCase {

    HomeSummaryResult getSummary(Long userId);
}
