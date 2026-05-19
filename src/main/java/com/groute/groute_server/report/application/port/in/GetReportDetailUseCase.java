package com.groute.groute_server.report.application.port.in;

import com.groute.groute_server.report.application.port.in.dto.ReportDetailView;

/**
 * RPT-001: 리포트 상세 조회 유스케이스.
 *
 * <p>리포트 단건을 조회하여 MINI/CAREER 타입에 맞는 상세 내용을 반환한다.
 */
public interface GetReportDetailUseCase {

    /**
     * @param reportId 조회할 리포트 ID
     * @param userId 현재 로그인한 유저 ID (소유자 검증용)
     * @return 리포트 상세 (MINI/CAREER 타입별 content 구조 상이)
     */
    ReportDetailView getDetail(Long reportId, Long userId);
}
