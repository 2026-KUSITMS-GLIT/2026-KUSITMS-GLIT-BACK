package com.groute.groute_server.record.adapter.in.web;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.record.adapter.in.web.dto.ScrumBulkWriteRequest;
import com.groute.groute_server.record.adapter.in.web.dto.ScrumBulkWriteResponse;
import com.groute.groute_server.record.adapter.in.web.dto.ScrumCompetencyUpdateRequest;
import com.groute.groute_server.record.adapter.in.web.dto.SyncDailyScrumRequest;
import com.groute.groute_server.record.application.port.in.scrum.BulkWriteScrumUseCase;
import com.groute.groute_server.record.application.port.in.scrum.SyncDailyScrumUseCase;
import com.groute.groute_server.record.application.port.in.scrum.UpdateScrumCompetencyUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Scrum", description = "스크럼 sync")
@RestController
@RequestMapping("/api/scrums")
@RequiredArgsConstructor
public class ScrumController {

    private final SyncDailyScrumUseCase syncDailyScrumUseCase;
    private final BulkWriteScrumUseCase bulkWriteScrumUseCase;
    private final UpdateScrumCompetencyUseCase updateScrumCompetencyUseCase;

    @Operation(
            summary = "스크럼 일괄 저장",
            description =
                    "심화 기록 플로우 진입 전 스크럼을 PENDING 상태로 일괄 저장한다. 응답으로 반환된 scrumId는 역량 선택 및 STAR 기록에 사용된다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "저장 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "필드 검증 실패 또는 날짜 형식 오류 또는 스크럼 5개 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "projectId가 본인 소유가 아님")
    })
    @PostMapping("/write")
    public ApiResponse<List<ScrumBulkWriteResponse>> bulkWrite(
            @CurrentUser Long userId, @Valid @RequestBody ScrumBulkWriteRequest request) {
        LocalDate date = DateParam.parseIso(request.date());
        List<ScrumBulkWriteResponse> response =
                bulkWriteScrumUseCase.bulkWrite(request.toCommand(userId, date)).groups().stream()
                        .map(ScrumBulkWriteResponse::from)
                        .toList();
        return ApiResponse.ok("스크럼 저장 성공", response);
    }

    @Operation(
            summary = "스크럼 역량 선택",
            description = "STAR 기록 시작 전 스크럼에 역량을 지정한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "역량 선택 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "필드 검증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "scrumId가 본인 소유가 아님")
    })
    @PatchMapping("/competencies")
    public ApiResponse<Void> updateCompetency(
            @CurrentUser Long userId,
            @Valid @RequestBody ScrumCompetencyUpdateRequest request) {
        updateScrumCompetencyUseCase.updateCompetency(request.toCommand(userId));
        return ApiResponse.ok("역량 선택 성공");
    }

    @Operation(
            summary = "일자별 스크럼 일괄 sync",
            description =
                    "지정한 일자(yyyy-MM-dd)의 스크럼을 요청 payload 상태로 동기화. titleId는 기존 ScrumTitle 참조,"
                            + " scrumId가 null이면 신규 생성, 요청에서 빠진 기존 스크럼은 삭제된다 (STAR cascade 포함).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "동기화 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "날짜 형식·필드 검증 실패 또는 일자당 5개 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "titleId 또는 scrumId가 본인 소유가 아님 / 다른 일자의 scrumId"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "14일 초과 또는 심화기록(STAR) 작성된 스크럼 수정·삭제 시도")
    })
    @PutMapping("/daily")
    public ApiResponse<Void> syncDailyScrum(
            @CurrentUser Long userId,
            @RequestParam("date") String dateRaw,
            @Valid @RequestBody SyncDailyScrumRequest request) {
        LocalDate date = DateParam.parseIso(dateRaw);
        syncDailyScrumUseCase.syncDailyScrum(request.toCommand(userId, date));
        return ApiResponse.ok("동기화 성공");
    }
}
