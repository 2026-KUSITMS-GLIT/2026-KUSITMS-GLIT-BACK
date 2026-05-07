package com.groute.groute_server.calendar.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

/**
 * 월별 캘린더 도메인 뷰.
 *
 * <p>해당 월의 1일부터 말일까지 모든 날짜가 포함되며, 데이터가 없는 날도 {@code hasScrums=false}, {@code hasStar=false}, {@code
 * primaryCategory=null}, {@code starCount=0}로 채운다. service ↔ controller 사이 전달용이며 응답 DTO는 controller
 * 계층에서 변환한다.
 */
public record CalendarMonthlyView(YearMonth month, List<DayAggregate> days) {

    public record DayAggregate(
            LocalDate date,
            boolean hasScrums,
            boolean hasStar,
            CompetencyCategory primaryCategory,
            int starCount) {}
}
