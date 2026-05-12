package com.groute.groute_server.report.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.validation.annotation.Validated;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportCreateRequest;
import com.groute.groute_server.report.adapter.in.web.dto.ReportCreateResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportDetailResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportGaugeResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportListResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportSelectableInfoResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportStatusResponse;
import com.groute.groute_server.report.application.port.in.CreateReportUseCase;
import com.groute.groute_server.report.application.port.in.GetReportDetailUseCase;
import com.groute.groute_server.report.application.port.in.GetReportGaugeUseCase;
import com.groute.groute_server.report.application.port.in.GetReportListUseCase;
import com.groute.groute_server.report.application.port.in.GetReportStatusUseCase;
import com.groute.groute_server.report.application.port.in.GetSelectableInfoUseCase;
import com.groute.groute_server.report.application.port.in.RetryReportUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 리포트 엔드포인트.
 *
 * <p>게이지·목록·상세 조회 및 사전 정보 조회 → 생성 요청 → 상태 폴링 → 재시도 플로우를 담당한다.
 */
@Tag(name = "Report", description = "리포트 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final GetReportGaugeUseCase getReportGaugeUseCase;
    private final GetReportListUseCase getReportListUseCase;
    private final GetReportDetailUseCase getReportDetailUseCase;
    private final GetSelectableInfoUseCase getSelectableInfoUseCase;
    private final CreateReportUseCase createReportUseCase;
    private final GetReportStatusUseCase getReportStatusUseCase;
    private final RetryReportUseCase retryReportUseCase;

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
        return ApiResponse.ok(ReportGaugeResponse.from(getReportGaugeUseCase.getGauge(userId)));
    }

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
        return ApiResponse.ok(ReportListResponse.from(getReportListUseCase.getList(userId)));
    }

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
        return ApiResponse.ok(
                ReportDetailResponse.from(getReportDetailUseCase.getDetail(reportId, userId)));
    }

    @Operation(
            summary = "리포트 생성용 사전 정보 조회",
            description = "미니/커리어 타입 판단 결과, 달력 하이라이트용 날짜 목록, 달력 화면 진입 시 자동으로 체크될 심화기록 ID 목록을 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @GetMapping("/selectable-info")
    public ApiResponse<ReportSelectableInfoResponse> getSelectableInfo(@CurrentUser Long userId) {
        return ApiResponse.ok(
                "기록 선택용 정보 조회 성공",
                ReportSelectableInfoResponse.from(
                        getSelectableInfoUseCase.getSelectableInfo(userId)));
    }

    @Operation(
            summary = "리포트 생성 요청",
            description = "유저가 선택한 심화기록을 바탕으로 리포트 생성을 요청한다. AI 생성은 비동기로 진행되며 생성된 reportId를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "생성 요청 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (미니 중복 생성, 심화기록 개수 오류)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @PostMapping
    public ApiResponse<ReportCreateResponse> createReport(
            @CurrentUser Long userId, @Validated @RequestBody ReportCreateRequest request) {
        return ApiResponse.ok(
                "리포트 생성 요청 성공",
                ReportCreateResponse.from(
                        createReportUseCase.createReport(request.toCommand(userId))));
    }

    @Operation(
            summary = "리포트 생성 상태 폴링",
            description = "AI 생성이 비동기라 프론트가 주기적으로 호출해 GENERATING/SUCCESS/FAILED 상태를 확인한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인의 리포트가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리포트를 찾을 수 없음")
    })
    @GetMapping("/{reportId}/status")
    public ApiResponse<ReportStatusResponse> getReportStatus(
            @CurrentUser Long userId, @PathVariable Long reportId) {
        return ApiResponse.ok(
                "리포트 생성 상태 조회 성공",
                ReportStatusResponse.from(
                        getReportStatusUseCase.getReportStatus(userId, reportId)));
    }

    @Operation(summary = "리포트 생성 재시도", description = "AI 생성 실패 시 1회에 한해 재시도를 요청한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "재시도 요청 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "재시도 불가 상태 (이미 재시도했거나 FAILED 상태가 아님)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인의 리포트가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리포트를 찾을 수 없음")
    })
    @PostMapping("/{reportId}/retry")
    public ApiResponse<ReportCreateResponse> retryReport(
            @CurrentUser Long userId, @PathVariable Long reportId) {
        return ApiResponse.ok(
                "리포트 생성 재시도 요청 성공",
                ReportCreateResponse.from(retryReportUseCase.retryReport(userId, reportId)));
    }
}
