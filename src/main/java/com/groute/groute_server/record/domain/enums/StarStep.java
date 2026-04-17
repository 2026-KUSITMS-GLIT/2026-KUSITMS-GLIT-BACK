package com.groute.groute_server.record.domain.enums;

/**
 * STAR 작성 단계
 */
public enum StarStep {
    ST,   // Situation·Task 작성 중
    A,    // Action 작성 중
    R,    // Result 작성 중
    DONE  // 3단계 완료, AI 태깅 호출 가능
}