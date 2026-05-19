package com.groute.groute_server.record.application.port.in.calendar;

/** 일자별 스크럼 전체 조회 유스케이스 (CAL-002, GET /api/calendar/daily). */
public interface GetDailyCalendarUseCase {

    DailyCalendarView getDailyCalendar(GetDailyCalendarQuery query);
}
