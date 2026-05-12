package com.groute.groute_server.report.adapter.in.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportSelectableRecordsResponse;
import com.groute.groute_server.report.application.port.in.GetSelectableRecordsUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 날짜별 심화기록 모달 조회 엔드포인트.
 *
 * <p>리포트 생성 화면에서 날짜 셀 클릭 시 해당 날짜의 완료된 심화기록 목록을 반환한다.
 */
@Tag(name = "Report", description = "리포트 생성 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportSelectableRecordsController {

    private final GetSelectableRecordsUseCase getSelectableRecordsUseCase;

    @Operation(
            summary = "날짜별 심화기록 모달 조회",
            description = "리포트 생성 화면에서 날짜 셀 클릭 시 해당 날짜의 완료된 심화기록 목록을 반환한다. 심화기록이 없으면 빈 배열을 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @GetMapping("/selectable-records/{date}")
    public ApiResponse<ReportSelectableRecordsResponse> getSelectableRecords(
            @CurrentUser Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(
                "날짜별 심화기록 모달 조회 성공",
                ReportSelectableRecordsResponse.from(
                        getSelectableRecordsUseCase.getSelectableRecords(userId, date)));
    }
}
