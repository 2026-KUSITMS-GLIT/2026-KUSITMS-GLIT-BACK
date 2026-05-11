package com.groute.groute_server.record.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.record.application.port.in.star.BulkCreateStarRecordResult;

import io.swagger.v3.oas.annotations.media.Schema;

/** 심화 기록 일괄 생성 응답 DTO. */
@Schema(description = "심화 기록 생성 응답")
public record StarRecordBulkCreateResponse(
        @Schema(description = "생성된 StarRecord 목록 (요청 순서와 동일)") List<StarRecordItem> items) {

    public static StarRecordBulkCreateResponse from(BulkCreateStarRecordResult result) {
        return new StarRecordBulkCreateResponse(
                result.starRecordIds().stream().map(StarRecordItem::new).toList());
    }

    @Schema(description = "StarRecord 항목")
    public record StarRecordItem(
            @Schema(description = "심화 STAR 식별자", example = "20000") Long starRecordId) {}
}
