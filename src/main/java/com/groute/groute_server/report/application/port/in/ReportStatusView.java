package com.groute.groute_server.report.application.port.in;

/**
 * 리포트 생성 상태 폴링(RPT-003) 응답 모델.
 *
 * @param reportId 리포트 PK
 * @param status GENERATING / SUCCESS / FAILED
 * @param retryAvailable FAILED 상태이고 재시도 가능하면 true, 그 외 null
 */
public record ReportStatusView(Long reportId, String status, Boolean retryAvailable) {}
