package com.groute.groute_server.record.domain.enums;

/**
 * STAR 심화 기록 작성 단계.
 * 재진입 시 이 단계부터 복원되어 임시저장 역할을 한다(REC005, REC010).
 */
public enum StarStep {
    /** Situation·Task 작성 중. */
    ST,
    /** Action 작성 중. */
    A,
    /** Result 작성 중. */
    R,
    /** 3단계 모두 완료. AI 태깅 호출 가능. */
    DONE
}
