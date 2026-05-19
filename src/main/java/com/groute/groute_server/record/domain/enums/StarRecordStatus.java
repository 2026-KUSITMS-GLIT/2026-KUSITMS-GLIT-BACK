package com.groute.groute_server.record.domain.enums;

/** StarRecord 작성·태깅 진행 상태. */
public enum StarRecordStatus {
    /** ST/A/R 단계 작성 중. 이전 단계 재저장 가능. */
    WRITING,
    /** R 단계 완료, AI 태깅 대기 중. 추가 작성 불가. */
    WRITTEN,
    /** AI 태깅 완료. COMMITTED 전환 체크 기준. */
    TAGGED
}
