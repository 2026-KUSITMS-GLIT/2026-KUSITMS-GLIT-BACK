package com.groute.groute_server.report.domain.enums;

/** 리포트 생성 상태. */
public enum ReportStatus {
    /** AI 호출 중. */
    GENERATING,
    /** 발행 완료. */
    SUCCESS,
    /** 생성 실패. 재시도 1회 제공 (RPT003). */
    FAILED
}
