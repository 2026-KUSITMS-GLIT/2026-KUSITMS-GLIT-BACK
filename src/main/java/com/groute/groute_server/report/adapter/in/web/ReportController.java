package com.groute.groute_server.report.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportDetailResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportGaugeResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportListResponse;
import com.groute.groute_server.report.application.port.in.GetReportDetailUseCase;
import com.groute.groute_server.report.application.port.in.GetReportGaugeUseCase;
import com.groute.groute_server.report.application.port.in.GetReportListUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 리포트 조회 엔드포인트 (RPT-001).
 *
 * <p>게이지·목록·상세 조회를 담당한다.
 */
@Tag(name = "Report", description = "리포트 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final GetReportGaugeUseCase getReportGaugeUseCase;
    private final GetReportListUseCase getReportListUseCase;
    private final GetReportDetailUseCase getReportDetailUseCase;

    /** RPT-001: 리포트 게이지 조회. */
    @Operation(summary = "리포트 게이지 조회", description = "마지막 리포트 생성 이후 완료된 심화기록 수와 다음 생성 임계치를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "게이지 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @GetMapping("/gauge")
    public ApiResponse<ReportGaugeResponse> getGauge(@CurrentUser Long userId) {
        return ApiResponse.ok(getReportGaugeUseCase.getGauge(userId));
    }

    /** RPT-001: 리포트 목록 조회. */
    @Operation(
            summary = "리포트 목록 조회",
            description = "유저의 리포트 목록을 생성일 기준 내림차순으로 반환한다. 이력 없으면 빈 배열 반환.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @GetMapping
    public ApiResponse<ReportListResponse> getList(@CurrentUser Long userId) {
        return ApiResponse.ok(getReportListUseCase.getList(userId));
    }

    /** RPT-001: 리포트 상세 조회. */
    @Operation(
            summary = "리포트 상세 조회",
            description = "리포트 단건을 조회한다. MINI/CAREER 타입에 따라 content 구조가 다르다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상세 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인 소유 리포트가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리포트를 찾을 수 없음")
    })
    @GetMapping("/{reportId}")
    public ApiResponse<ReportDetailResponse> getDetail(
            @PathVariable Long reportId, @CurrentUser Long userId) {
        return ApiResponse.ok(getReportDetailUseCase.getDetail(reportId, userId));
    }
}
