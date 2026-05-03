package com.groute.groute_server.record.application.port.in;

import com.groute.groute_server.record.adapter.in.web.dto.AiTaggingResultResponse;

/**
 * REC-007: AI 태깅 결과 조회 유스케이스.
 *
 * <p>잡 상태가 SUCCESS일 때만 결과를 반환한다. SUCCESS가 아니면 {@code AI_TAGGING_NOT_COMPLETED(400)} 예외를 던진다.
 */
public interface GetAiTaggingResultUseCase {

    /**
     * @param starRecordId 결과를 조회할 STAR 기록 ID
     * @param userId 현재 로그인한 유저 ID (소유자 검증용)
     * @return AI 태깅 결과 (primaryCategory + detailTags)
     */
    AiTaggingResultResponse getResult(Long starRecordId, Long userId);
}
