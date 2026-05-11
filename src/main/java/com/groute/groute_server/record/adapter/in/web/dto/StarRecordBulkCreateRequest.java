package com.groute.groute_server.record.adapter.in.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.groute.groute_server.record.application.port.in.star.BulkCreateStarRecordCommand;

import io.swagger.v3.oas.annotations.media.Schema;

/** 심화 기록 일괄 생성 요청 DTO (POST /api/star-records/bulk). */
@Schema(description = "심화 기록 생성 요청")
public record StarRecordBulkCreateRequest(
        @NotNull @NotEmpty @Valid @Schema(description = "STAR 기록할 스크럼 목록") List<ScrumItem> items) {

    public BulkCreateStarRecordCommand toCommand(Long userId) {
        return new BulkCreateStarRecordCommand(
                userId, items.stream().map(ScrumItem::scrumId).toList());
    }

    @Schema(description = "스크럼 항목")
    public record ScrumItem(
            @NotNull @Schema(description = "스크럼 식별자", example = "1000") Long scrumId) {}
}
