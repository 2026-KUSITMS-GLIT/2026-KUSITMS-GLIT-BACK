package com.groute.groute_server.home.controller;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.home.dto.CompetencyStatsResponse;
import com.groute.groute_server.home.service.HomeCompetencyStatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 홈 역량 잔디 조회 컨트롤러(HOM-002).
 *
 * <p>월 단위 STAR 완료 건수를 일자별 히트맵 데이터로 반환한다. 인증 사용자 본인의 데이터만 집계하며 {@link CurrentUser}로 userId를 주입받는다.
 * {@code month} 쿼리 파라미터는 {@link YearMonth}로 바인딩되어 형식 위반 시 Spring이 자동 400 응답을 반환한다.
 *
 * <p>서비스 단은 도메인 원시값(Map)만 노출하므로 응답 DTO 조립은 본 컨트롤러 책임이다.
 */
@Tag(name = "Home", description = "홈 화면 데이터 조회")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeCompetencyStatsController {

    private final HomeCompetencyStatsService homeCompetencyStatsService;

    @Operation(
            summary = "월별 역량 잔디 조회",
            description =
                    "요청 월의 일자별 STAR 완료 건수를 4단계 상태(NO_DATA/STAR_LOW/STAR_MID/STAR_HIGH)로 매핑해 반환한다."
                            + " 데이터 없는 일자도 NO_DATA로 응답에 포함되며, 응답 days[]는 요청 월의 1일~말일을 모두 담는다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "month 파라미터 누락 또는 형식 위반(yyyy-MM)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @GetMapping("/competency-stats")
    public ApiResponse<CompetencyStatsResponse> getCompetencyStats(
            @CurrentUser Long userId,
            @Parameter(description = "조회 월 (yyyy-MM)", example = "2026-04", required = true)
                    @RequestParam("month")
                    @DateTimeFormat(pattern = "yyyy-MM")
                    YearMonth month) {
        return ApiResponse.ok(
                "역량 잔디 조회 성공",
                CompetencyStatsResponse.from(
                        month,
                        homeCompetencyStatsService.getCompletedStarCountsByMonth(userId, month)));
    }
}
