package com.groute.groute_server.record.application.port.in;

/**
 * AI 태깅 완료 처리 유스케이스.
 *
 * <p>FastAPI가 태깅을 완료한 뒤 호출한다. StarRecord를 TAGGED로 전환하고, 세션 내 모든 기록이 완료되면 ScrumTitle을 COMMITTED로
 * 전환한다.
 */
public interface CompleteAiTaggingUseCase {

    void completeTagging(Long starRecordId);
}
