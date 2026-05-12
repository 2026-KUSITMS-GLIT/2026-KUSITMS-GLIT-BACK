package com.groute.groute_server.report.application.port.in;

/** 리포트 생성 재시도 유스케이스. AI 생성 실패 시 1회에 한해 재시도를 제공한다. */
public interface RetryReportUseCase {

    /**
     * @return 재시도 대상 리포트 ID
     */
    Long retryReport(Long userId, Long reportId);
}
