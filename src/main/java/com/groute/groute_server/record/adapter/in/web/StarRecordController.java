package com.groute.groute_server.record.adapter.in.web;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.record.adapter.in.web.dto.StarDetailResponse;
import com.groute.groute_server.record.adapter.in.web.dto.StarRecordBulkCreateRequest;
import com.groute.groute_server.record.adapter.in.web.dto.StarRecordBulkCreateResponse;
import com.groute.groute_server.record.application.port.in.star.BulkCreateStarRecordUseCase;
import com.groute.groute_server.record.application.port.in.star.DeleteStarCommand;
import com.groute.groute_server.record.application.port.in.star.DeleteStarUseCase;
import com.groute.groute_server.record.application.port.in.star.GetStarDetailQuery;
import com.groute.groute_server.record.application.port.in.star.GetStarDetailUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 심화기록 상세 조회·단독 삭제(CAL-003) 엔드포인트.
 *
 * <p>본 페이지는 읽기 전용이며 수정 API는 제공하지 않는다. 삭제 시 STAR는 soft-delete 되고 연결된 Scrum의 hasStar 플래그만 false로
 * 동기화된다 (스크럼 본문 보존).
 */
@Tag(name = "StarRecord", description = "심화기록 상세 조회·단독 삭제")
@RestController
@RequestMapping("/api/star-records")
@RequiredArgsConstructor
public class StarRecordController {

    private final BulkCreateStarRecordUseCase bulkCreateStarRecordUseCase;
    private final GetStarDetailUseCase getStarDetailUseCase;
    private final DeleteStarUseCase deleteStarUseCase;

    @Operation(
            summary = "심화 기록 일괄 생성",
            description =
                    "STAR 기록할 스크럼을 선택하면 스크럼마다 PENDING 상태의 StarRecord를 생성한다. 역량 선택 전에 호출해야 한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "중복 scrumId 또는 빈 목록"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "스크럼을 찾을 수 없음 (미존재 또는 타인 소유)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "이미 STAR가 완료된 스크럼 포함")
    })
    @PostMapping("/bulk")
    public ApiResponse<StarRecordBulkCreateResponse> bulkCreate(
            @CurrentUser Long userId, @RequestBody @Valid StarRecordBulkCreateRequest request) {
        return ApiResponse.ok(
                "심화 기록 생성 성공",
                StarRecordBulkCreateResponse.from(
                        bulkCreateStarRecordUseCase.bulkCreate(request.toCommand(userId))));
    }

    @Operation(
            summary = "심화기록 상세 조회",
            description =
                    "심화기록 본문(S·T/A/R) + 카테고리·자유작성 + 대표 역량/세부 태그 + 첨부 이미지 목록을 반환한다."
                            + " 이미지·세부 태그가 없으면 빈 배열로 응답한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인의 심화기록이 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "심화기록을 찾을 수 없음")
    })
    @GetMapping("/{starRecordId}")
    public ApiResponse<StarDetailResponse> getStarDetail(
            @CurrentUser Long userId, @PathVariable Long starRecordId) {
        return ApiResponse.ok(
                "심화 기록 조회 성공",
                StarDetailResponse.from(
                        getStarDetailUseCase.getStarDetail(
                                new GetStarDetailQuery(userId, starRecordId))));
    }

    @Operation(
            summary = "심화기록 단독 삭제",
            description =
                    "STAR를 soft-delete 하고 연결된 Scrum의 hasStar 플래그를 false로 동기화한다. 스크럼 본문은 보존된다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인의 심화기록이 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "심화기록을 찾을 수 없음")
    })
    @DeleteMapping("/{starRecordId}")
    public ApiResponse<Void> deleteStar(@CurrentUser Long userId, @PathVariable Long starRecordId) {
        deleteStarUseCase.deleteStar(new DeleteStarCommand(userId, starRecordId));
        return ApiResponse.ok("심화 기록 삭제 성공");
    }
}
