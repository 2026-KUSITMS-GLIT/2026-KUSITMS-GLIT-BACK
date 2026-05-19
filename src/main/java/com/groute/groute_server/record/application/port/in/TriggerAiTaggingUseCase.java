package com.groute.groute_server.record.application.port.in;

/**
 * REC-005: AI 태깅 트리거 유스케이스.
 *
 * <p>STAR R단계 완료 후 호출. ai_tagging_jobs 큐에 잡을 생성한다.
 *
 * <ul>
 *   <li>기존 잡 없거나 FAILED {@code &&} retryCount=0 → 새 잡 생성 (201)
 *   <li>기존 잡 RUNNING → 409 Conflict
 *   <li>기존 잡 FAILED {@code &&} retryCount=1 → 400 Bad Request (최종 실패, 재시도 불가)
 * </ul>
 */
public interface TriggerAiTaggingUseCase {

    /**
     * @param starRecordId AI 태깅을 요청할 STAR 기록 ID
     * @param userId 현재 로그인한 유저 ID (소유자 검증용)
     */
    void trigger(Long starRecordId, Long userId);
}
