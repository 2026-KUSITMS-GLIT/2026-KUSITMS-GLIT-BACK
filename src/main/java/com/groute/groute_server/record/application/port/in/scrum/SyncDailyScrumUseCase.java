package com.groute.groute_server.record.application.port.in.scrum;

/**
 * 일자별 스크럼 일괄 sync 유스케이스 (PUT /api/scrums/daily).
 *
 * <p>응답 본문은 비운다(성공 메시지만). 클라이언트는 후속 GET으로 최신 목록을 조회한다.
 */
public interface SyncDailyScrumUseCase {

    void syncDailyScrum(SyncDailyScrumCommand command);
}
