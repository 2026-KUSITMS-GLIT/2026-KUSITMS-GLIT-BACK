package com.groute.groute_server.record.application.port.in;

import java.util.List;

/**
 * AI 태깅 완료 처리 유스케이스.
 *
 * <p>FastAPI가 태깅을 완료한 뒤 호출한다. StarRecord를 TAGGED로 전환하고, 세션 내 모든 기록이 완료되면 ScrumTitle을 COMMITTED로
 * 전환한다.
 */
public interface CompleteAiTaggingUseCase {

    /**
     * @param starRecordId 완료된 STAR 기록 ID
     * @param primaryCategory AI가 추출한 대표 역량 (e.g. "PLANNING_EXECUTION")
     * @param detailTags AI가 추출한 세부 태그 목록 (1~3개)
     */
    void completeTagging(Long starRecordId, String primaryCategory, List<String> detailTags);

    /**
     * AI 태깅 최종 실패 시 StarRecord.isCompleted를 false로 롤백한다.
     *
     * <p>재시도 포함 모든 시도가 실패로 끝났을 때 호출한다. 롤백 후 해당 StarRecord는 캘린더·리포트 카운트에서 제외된다.
     */
    void revertTaggingComplete(Long starRecordId);
}
