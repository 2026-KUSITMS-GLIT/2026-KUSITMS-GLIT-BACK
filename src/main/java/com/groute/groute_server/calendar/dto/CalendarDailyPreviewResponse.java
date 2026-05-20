package com.groute.groute_server.calendar.dto;

import java.util.List;

import com.groute.groute_server.calendar.service.CalendarDailyPreviewView;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 캘린더 메인 홈 날짜 프리뷰 조회 응답 DTO.
 *
 * <p>지정 일자의 작업(ScrumTitle) 단위 프리뷰 목록을 반환한다. 같은 freeText에 속한 스크럼들이 하나의 카드로 묶이고, 카드 단위로 STAR 완료된
 * 스크럼들의 {@code primaryCategory}가 distinct 배열로 노출된다.
 */
@Schema(description = "날짜 프리뷰 조회 응답")
public record CalendarDailyPreviewResponse(
        @Schema(description = "조회한 날짜 (yyyy-MM-dd)", example = "2026-05-20") String date,
        @Schema(description = "작업(title) 단위 프리뷰 목록 (없으면 빈 배열)") List<TitlePreview> titles) {

    public static CalendarDailyPreviewResponse from(CalendarDailyPreviewView view) {
        List<TitlePreview> titles =
                view.titles().stream()
                        .map(
                                t ->
                                        new TitlePreview(
                                                t.titleId(),
                                                t.projectName(),
                                                t.freeText(),
                                                t.primaryCategories(),
                                                t.scrumCount(),
                                                t.hasStarAny()))
                        .toList();
        return new CalendarDailyPreviewResponse(view.date().toString(), titles);
    }

    @Schema(description = "작업(title) 단위 프리뷰 항목")
    public record TitlePreview(
            @Schema(description = "스크럼 제목 식별자", example = "50") Long titleId,
            @Schema(description = "프로젝트 태그명 (최대 15자)", example = "밋업프로젝트") String projectName,
            @Schema(description = "제목 자유작성 영역 (최대 20자)", example = "기획 작업") String freeText,
            @Schema(
                            description =
                                    "카드 내 STAR 완료된 스크럼들의 distinct primaryCategory 집합. STAR 완료가 없으면 빈 배열.",
                            example = "[\"PLANNING_EXECUTION\", \"COLLABORATION\"]")
                    List<CompetencyCategory> primaryCategories,
            @Schema(description = "카드에 속한 스크럼 개수", example = "2") int scrumCount,
            @Schema(description = "카드 내 STAR 완료된 스크럼이 1건 이상 존재하는지 여부", example = "true")
                    boolean hasStarAny) {}
}
