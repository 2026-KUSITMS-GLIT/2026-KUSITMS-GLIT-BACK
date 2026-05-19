package com.groute.groute_server.report.application.port.in;

/** 리포트 생성 상태 폴링 유스케이스. AI 생성이 비동기라 프론트가 주기적으로 호출해 GENERATING/SUCCESS/FAILED 상태를 확인한다. */
public interface GetReportStatusUseCase {

    ReportStatusView getReportStatus(Long userId, Long reportId);
}
