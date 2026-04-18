package com.groute.groute_server.record.domain.enums;

/**
 * AI 태깅 비동기 잡 상태.
 * 워커가 SELECT FOR UPDATE SKIP LOCKED로 QUEUED 잡을 폴링한다.
 */
public enum JobStatus {
    /** 대기 중. 워커가 폴링하는 대상. */
    QUEUED,
    /** 워커가 처리 중. */
    RUNNING,
    /** 성공. */
    SUCCESS,
    /** 실패. retry_count=0이면 1차 실패(재시도 가능), retry_count=1이면 최종 실패. */
    FAILED
}
