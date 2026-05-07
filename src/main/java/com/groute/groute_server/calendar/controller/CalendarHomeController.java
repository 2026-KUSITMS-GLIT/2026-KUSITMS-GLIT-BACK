package com.groute.groute_server.calendar.controller;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.calendar.dto.CalendarDailyPreviewResponse;
import com.groute.groute_server.calendar.dto.CalendarMonthlyResponse;
import com.groute.groute_server.calendar.service.CalendarHomeService;
import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 캘린더 메인 홈 조회 엔드포인트(CAL-001).
 *
 * <p>월별 잔디(스크럼/STAR 작성 + 대표 역량) 데이터와 날짜 클릭 시 노출되는 프리뷰 목록을 반환한다. 인증된 사용자(JWT) 본인 데이터만 조회.
 */
@Tag(name = "Calendar", description = "일자별 스크럼 조회")
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarHomeController {

    private final CalendarHomeService calendarHomeService;

    @Operation(
            summary = "월별 캘린더 데이터 조회",
            description =
                    "지정한 연월(yyyy-MM)의 일별 데이터(스크럼/STAR 작성 여부, 대표 역량, STAR 완료 건수)를 1일~말일 모두 포함해 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "month 형식이 올바르지 않음 (yyyy-MM)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @GetMapping("/monthly")
    public ApiResponse<CalendarMonthlyResponse> getMonthly(
            @CurrentUser Long userId,
            @RequestParam("month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ApiResponse.ok(
                CalendarMonthlyResponse.from(calendarHomeService.getMonthly(userId, month)));
    }

    @Operation(
            summary = "날짜 프리뷰 조회",
            description =
                    "지정한 일자(yyyy-MM-dd)의 스크럼 목록을 반환한다. STAR 완료 시에만 대표 역량/세부 태그가 채워지며 그 외 null.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "date 형식이 올바르지 않음 (yyyy-MM-dd)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @GetMapping("/daily-preview")
    public ApiResponse<CalendarDailyPreviewResponse> getDailyPreview(
            @CurrentUser Long userId,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ApiResponse.ok(
                CalendarDailyPreviewResponse.from(
                        calendarHomeService.getDailyPreview(userId, date)));
    }
}
