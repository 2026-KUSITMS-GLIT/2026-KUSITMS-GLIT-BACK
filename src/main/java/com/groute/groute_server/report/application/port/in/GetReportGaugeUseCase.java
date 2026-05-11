package com.groute.groute_server.report.application.port.in;

import com.groute.groute_server.report.application.port.in.dto.ReportGaugeView;

/**
 * RPT-001: 리포트 게이지 조회 유스케이스.
 *
 * <p>마지막 리포트 생성 이후 완료된 심화기록 수와 다음 생성 임계치를 반환한다.
 */
public interface GetReportGaugeUseCase {

    /**
     * @param userId 현재 로그인한 유저 ID
     * @return 게이지 정보 (currentCount, nextThreshold, progressRate, isGeneratable)
     */
    ReportGaugeView getGauge(Long userId);
}
