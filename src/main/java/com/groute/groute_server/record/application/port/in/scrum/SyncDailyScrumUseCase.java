package com.groute.groute_server.record.application.port.in.scrum;

import com.groute.groute_server.record.application.port.in.calendar.DailyCalendarView;

/**
 * 일자별 스크럼 일괄 sync 유스케이스 (PUT /api/scrums/daily).
 *
 * <p>응답은 sync 후 전체 목록으로, {@link
 * com.groute.groute_server.record.application.port.in.calendar.GetDailyCalendarUseCase}와 동일 schema를
 * 반환한다.
 */
public interface SyncDailyScrumUseCase {

    DailyCalendarView syncDailyScrum(SyncDailyScrumCommand command);
}
