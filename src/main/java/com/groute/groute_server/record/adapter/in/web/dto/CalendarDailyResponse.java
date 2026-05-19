package com.groute.groute_server.record.adapter.in.web.dto;

import java.util.List;

import com.groute.groute_server.record.application.port.in.calendar.DailyCalendarView;

import io.swagger.v3.oas.annotations.media.Schema;

/** 일자별 스크럼 조회 응답 DTO. {@link DailyCalendarView}의 Web 표현. */
@Schema(description = "일자별 스크럼 조회 응답")
public record CalendarDailyResponse(
        @Schema(description = "ScrumTitle 단위 그룹 목록 (없으면 빈 배열)") List<GroupResponse> groups) {

    public static CalendarDailyResponse from(DailyCalendarView view) {
        return new CalendarDailyResponse(view.groups().stream().map(GroupResponse::from).toList());
    }

    @Schema(description = "ScrumTitle 그룹")
    public record GroupResponse(
            @Schema(description = "스크럼 제목 ID", example = "12") Long titleId,
            @Schema(description = "프로젝트 태그 (최대 15자)", example = "밋업 프로젝트") String projectTag,
            @Schema(description = "자유작성 (최대 20자)", example = "기획 작업") String freeText,
            @Schema(description = "그룹 내 수정 가능한 항목이 하나라도 있는지", example = "true") boolean isEditable,
            @Schema(description = "그룹 내 항목 목록") List<ItemResponse> items) {

        static GroupResponse from(DailyCalendarView.GroupView group) {
            return new GroupResponse(
                    group.titleId(),
                    group.projectTag(),
                    group.freeText(),
                    group.isEditable(),
                    group.items().stream().map(ItemResponse::from).toList());
        }
    }

    @Schema(description = "스크럼 항목")
    public record ItemResponse(
            @Schema(description = "스크럼 ID", example = "37") Long scrumId,
            @Schema(description = "본문 (최대 50자)", example = "와이어프레임 설계") String content,
            @Schema(description = "심화기록(STAR) 작성 여부", example = "false") boolean hasStar,
            @Schema(description = "수정 가능 여부 (14일 이내 + hasStar=false)", example = "true")
                    boolean isEditable) {

        static ItemResponse from(DailyCalendarView.ItemView item) {
            return new ItemResponse(
                    item.scrumId(), item.content(), item.hasStar(), item.isEditable());
        }
    }
}
