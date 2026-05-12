package com.groute.groute_server.report.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.report.application.port.in.SelectableRecordsView;

import io.swagger.v3.oas.annotations.media.Schema;

/** 날짜별 심화기록 모달 조회 응답 DTO. {@link SelectableRecordsView}의 Web 표현. */
@Schema(description = "날짜별 심화기록 모달 조회 응답")
public record ReportSelectableRecordsResponse(
        @Schema(description = "조회 날짜", example = "2026-04-09") String date,
        @Schema(description = "해당 날짜에 완료된 심화기록 목록. 기록 순서 기준 오름차순")
                List<StarRecordItem> starRecords) {

    @Schema(description = "심화기록 항목")
    public record StarRecordItem(
            @Schema(description = "심화기록 식별자", example = "11") Long starRecordId,
            @Schema(description = "프로젝트명", example = "KOPLE") String projectName,
            @Schema(description = "해당 심화기록이 속한 스크럼 본문", example = "PRD와 기능명세서를 완성")
                    String scrumContent) {

        public static StarRecordItem from(SelectableRecordsView.StarRecordItem item) {
            return new StarRecordItem(item.starRecordId(), item.projectName(), item.scrumContent());
        }
    }

    public static ReportSelectableRecordsResponse from(SelectableRecordsView view) {
        return new ReportSelectableRecordsResponse(
                view.date(), view.starRecords().stream().map(StarRecordItem::from).toList());
    }
}
