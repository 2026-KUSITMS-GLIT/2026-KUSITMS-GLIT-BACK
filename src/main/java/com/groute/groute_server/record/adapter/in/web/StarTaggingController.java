package com.groute.groute_server.record.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.record.adapter.in.web.dto.AiTaggingResultResponse;
import com.groute.groute_server.record.adapter.in.web.dto.AiTaggingStatusResponse;
import com.groute.groute_server.record.application.port.in.GetAiTaggingResultUseCase;
import com.groute.groute_server.record.application.port.in.GetAiTaggingStatusUseCase;
import com.groute.groute_server.record.application.port.in.TriggerAiTaggingUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * AI 태깅 트리거·상태 폴링·결과 조회 엔드포인트.
 *
 * <p>STAR R단계 완료 후 AI 역량 태깅을 비동기로 처리한다 (REC-005~007). 모든 엔드포인트는 로그인 사용자 본인 리소스만 다루므로 {@link
 * CurrentUser}로 userId를 주입받는다.
 */
@Tag(name = "Record", description = "심화 기록(STAR) AI 태깅 API")
@RestController
@RequestMapping("/api/star-records")
@RequiredArgsConstructor
public class StarTaggingController {

    private final TriggerAiTaggingUseCase triggerAiTaggingUseCase;
    private final GetAiTaggingStatusUseCase getAiTaggingStatusUseCase;
    private final GetAiTaggingResultUseCase getAiTaggingResultUseCase;

    /**
     * REC-005: AI 태깅 트리거.
     *
     * <p>STAR R단계 완료 후 호출. ai_tagging_jobs 큐에 잡을 생성한다.
     */
    @Operation(
            summary = "AI 태깅 트리거",
            description = "STAR R단계 완료 후 AI 역량 태깅을 요청한다. 첫 태깅 시도 실패 시 재호출 가능.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "AI 태깅 잡 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "STAR 작성 미완료 또는 최종 실패(재시도 불가)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인 소유 STAR 기록이 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "STAR 기록을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "AI 태깅이 이미 진행 중")
    })
    @PostMapping("/{starRecordId}/ai-tagging")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> triggerAiTagging(
            @PathVariable Long starRecordId, @CurrentUser Long userId) {
        triggerAiTaggingUseCase.trigger(starRecordId, userId);
        return ApiResponse.created("AI 태깅 트리거 성공");
    }

    /**
     * REC-006: AI 태깅 상태 폴링.
     *
     * <p>프론트가 주기적으로 호출하여 잡 상태를 확인한다.
     */
    @Operation(
            summary = "AI 태깅 상태 폴링",
            description =
                    "AI 태깅 잡의 현재 상태와 재시도 횟수를 반환한다. FAILED && retryCount=0이면 재시도 가능, retryCount=1이면 최종 실패.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상태 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인 소유 STAR 기록이 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "STAR 기록 또는 잡을 찾을 수 없음")
    })
    @GetMapping("/{starRecordId}/ai-tagging/status")
    public ApiResponse<AiTaggingStatusResponse> getAiTaggingStatus(
            @PathVariable Long starRecordId, @CurrentUser Long userId) {
        AiTaggingStatusResponse response =
                getAiTaggingStatusUseCase.getStatus(starRecordId, userId);
        return ApiResponse.ok(response);
    }

    /**
     * REC-007: AI 태깅 결과 조회.
     *
     * <p>잡 상태가 SUCCESS일 때만 결과를 반환한다.
     */
    @Operation(
            summary = "AI 태깅 결과 조회",
            description = "AI 태깅이 완료된 경우 대표 역량과 세부 태그를 반환한다. SUCCESS 상태가 아니면 400을 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "결과 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "AI 태깅이 아직 완료되지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인 소유 STAR 기록이 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "STAR 기록 또는 잡을 찾을 수 없음")
    })
    @GetMapping("/{starRecordId}/ai-tagging/result")
    public ApiResponse<AiTaggingResultResponse> getAiTaggingResult(
            @PathVariable Long starRecordId, @CurrentUser Long userId) {
        AiTaggingResultResponse response =
                getAiTaggingResultUseCase.getResult(starRecordId, userId);
        return ApiResponse.ok(response);
    }
}
