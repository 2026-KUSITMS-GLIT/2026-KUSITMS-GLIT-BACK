package com.groute.groute_server.report.application.port.in;

import com.groute.groute_server.report.application.port.in.dto.ReportListView;

/**
 * RPT-001: 리포트 목록 조회 유스케이스.
 *
 * <p>유저의 리포트 목록을 생성일 기준 내림차순으로 반환한다. 이력 없으면 빈 배열 반환.
 */
public interface GetReportListUseCase {

    /**
     * @param userId 현재 로그인한 유저 ID
     * @return 리포트 목록 (생성일 내림차순)
     */
    ReportListView getList(Long userId);
}
