package com.groute.groute_server.report.domain.enums;

/**
 * 리포트 생성 상태
 */
public enum ReportStatus {
    GENERATING, // AI 호출 중
    SUCCESS,    // 발행 완료
    FAILED      // 재시도 1회 제공
}