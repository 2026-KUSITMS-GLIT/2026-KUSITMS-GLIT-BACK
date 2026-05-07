package com.groute.groute_server.calendar.dto;

import java.util.List;

import com.groute.groute_server.calendar.service.CalendarMonthlyView;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 캘린더 메인 홈 월별 조회 응답 DTO.
 *
 * <p>해당 월의 1일부터 말일까지 모든 일자가 포함되며, 데이터가 없는 날도 {@code hasScrums=false}, {@code hasStar=false}, {@code
 * primaryCategory=null}, {@code starCount=0}로 표현된다.
 */
@Schema(description = "월별 캘린더 데이터 조회 응답")
public record CalendarMonthlyResponse(
        @Schema(description = "조회한 연월 (yyyy-MM)", example = "2026-04") String month,
        @Schema(description = "일별 캘린더 데이터 목록 (해당 월 1일~말일 모두 포함)") List<DayInfo> days) {

    public static CalendarMonthlyResponse from(CalendarMonthlyView view) {
        List<DayInfo> dayInfos =
                view.days().stream()
                        .map(
                                d ->
                                        new DayInfo(
                                                d.date().toString(),
                                                d.hasScrums(),
                                                d.hasStar(),
                                                d.primaryCategory(),
                                                d.starCount()))
                        .toList();
        return new CalendarMonthlyResponse(view.month().toString(), dayInfos);
    }

    @Schema(description = "일별 캘린더 데이터")
    public record DayInfo(
            @Schema(description = "날짜 (yyyy-MM-dd)", example = "2026-04-02") String date,
            @Schema(description = "스크럼 작성 여부", example = "true") boolean hasScrums,
            @Schema(description = "STAR 완료 여부", example = "true") boolean hasStar,
            @Schema(
                            description = "대표 역량. STAR 완료 기록이 없으면 null",
                            example = "PLANNING_EXECUTION",
                            nullable = true)
                    CompetencyCategory primaryCategory,
            @Schema(description = "해당 일자의 STAR 완료 건수", example = "2") int starCount) {}
}
