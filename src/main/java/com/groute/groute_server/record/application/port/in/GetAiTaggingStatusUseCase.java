package com.groute.groute_server.record.application.port.in;

import com.groute.groute_server.record.adapter.in.web.dto.AiTaggingStatusResponse;

/**
 * REC-006: AI 태깅 상태 폴링 유스케이스.
 *
 * <p>프론트가 주기적으로 호출하여 잡 상태를 확인한다.
 * 반환되는 status와 retryCount로 프론트가 재시도 여부를 판단한다.
 *
 * <ul>
 *   <li>{@code status=FAILED && retryCount=0} → 프론트가 REC-005 재호출 (1차 실패, 재시도 가능)
 *   <li>{@code status=FAILED && retryCount=1} → 최종 실패 화면 노출
 * </ul>
 */
public interface GetAiTaggingStatusUseCase {

    /**
     * @param starRecordId 상태를 조회할 STAR 기록 ID
     * @param userId 현재 로그인한 유저 ID (소유자 검증용)
     * @return 잡 상태 및 재시도 횟수
     */
    AiTaggingStatusResponse getStatus(Long starRecordId, Long userId);
}
