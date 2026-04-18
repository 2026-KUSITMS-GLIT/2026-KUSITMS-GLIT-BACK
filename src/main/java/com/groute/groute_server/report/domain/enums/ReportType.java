package com.groute.groute_server.report.domain.enums;

/**
 * 리포트 타입.
 * MVP에서는 결제 없이 무료 생성만 제공.
 */
public enum ReportType {
    /** 미니 리포트. 첫 심화기록 10회 누적 시 1회성 발행 (무료). */
    MINI,
    /** 커리어 리포트. 10회 이후 10회 단위 누적마다 생성 가능 (MVP 무료). */
    CAREER
}
