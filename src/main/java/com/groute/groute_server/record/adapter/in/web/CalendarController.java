package com.groute.groute_server.record.adapter.in.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.record.adapter.in.web.dto.CalendarDailyResponse;
import com.groute.groute_server.record.application.port.in.calendar.GetDailyCalendarQuery;
import com.groute.groute_server.record.application.port.in.calendar.GetDailyCalendarUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 일자별 캘린더 조회(CAL-002) 엔드포인트.
 *
 * <p>로그인 사용자 본인의 해당 일자 스크럼 전체를 ScrumTitle 단위로 그룹핑하여 반환한다.
 */
@Tag(name = "Calendar", description = "일자별 스크럼 조회")
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final GetDailyCalendarUseCase getDailyCalendarUseCase;

    @Operation(
            summary = "일자별 스크럼 조회",
            description = "지정한 일자(yyyy-MM-dd)의 스크럼을 ScrumTitle 단위로 그룹핑해 반환. 스크럼이 없으면 빈 배열을 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "날짜 형식이 올바르지 않음 (yyyy-MM-dd)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @GetMapping("/daily")
    public ApiResponse<CalendarDailyResponse> getDailyCalendar(
            @CurrentUser Long userId, @RequestParam("date") String dateRaw) {
        LocalDate date = parseDate(dateRaw);
        return ApiResponse.ok(
                CalendarDailyResponse.from(
                        getDailyCalendarUseCase.getDailyCalendar(
                                new GetDailyCalendarQuery(userId, date))));
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.CALENDAR_INVALID_DATE_FORMAT);
        }
    }
}
