package com.groute.groute_server.record.application.port.in.calendar;

import java.time.LocalDate;

/** 일자별 캘린더 조회 입력. */
public record GetDailyCalendarQuery(Long userId, LocalDate date) {}
