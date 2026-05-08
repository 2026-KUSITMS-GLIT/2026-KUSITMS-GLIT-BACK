package com.groute.groute_server.record.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.record.application.port.in.scrum.BulkWriteScrumResult;

import io.swagger.v3.oas.annotations.media.Schema;

/** 스크럼 일괄 저장 응답 DTO. */
@Schema(description = "스크럼 제목 단위 저장 결과")
public record ScrumBulkWriteResponse(
        @Schema(description = "프로젝트 이름 (최대 15자)", example = "밋업프로젝트") String projectName,
        @Schema(description = "자유 입력란 (최대 20자)", example = "4/22 기획 작업") String freeText,
        @Schema(description = "저장된 스크럼 목록") List<ScrumItem> scrums) {

    public static ScrumBulkWriteResponse from(BulkWriteScrumResult.GroupResult result) {
        List<ScrumItem> items =
                result.scrums().stream().map(s -> new ScrumItem(s.scrumId(), s.content())).toList();
        return new ScrumBulkWriteResponse(result.projectName(), result.freeText(), items);
    }

    @Schema(description = "스크럼 항목")
    public record ScrumItem(
            @Schema(description = "스크럼 식별자", example = "1") Long scrumId,
            @Schema(description = "스크럼 내용 (최대 50자)", example = "유저 리서치 문항 설계") String content) {}
}