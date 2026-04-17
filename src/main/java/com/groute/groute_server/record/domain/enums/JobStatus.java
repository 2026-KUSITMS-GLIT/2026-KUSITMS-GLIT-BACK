package com.groute.groute_server.record.domain.enums;

/**
 * 비동기 작업 상태
 */
public enum JobStatus {
    QUEUED,  // 대기 중. 워커가 폴링
    RUNNING, // 워커가 처리 중
    SUCCESS, // 성공
    FAILED   // 실패 (retry_count=0이면 1차, retry_count=1이면 최종)
}