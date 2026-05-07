package com.groute.groute_server.calendar.dto;

import java.util.List;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 캘린더 메인 홈 날짜 프리뷰 조회 응답 DTO.
 *
 * <p>지정 일자의 스크럼 목록을 반환한다. 각 스크럼은 STAR 완료 시에만 {@code primaryCategory}/{@code detailTags}가 채워지며,
 * 미완료/미작성이면 두 필드 모두 {@code null}.
 */
@Schema(description = "날짜 프리뷰 조회 응답")
public record CalendarDailyPreviewResponse(
        @Schema(description = "조회한 날짜 (yyyy-MM-dd)", example = "2026-04-02") String date,
        @Schema(description = "스크럼 목록 (없으면 빈 배열)") List<ScrumPreview> scrums) {

    @Schema(description = "스크럼 프리뷰 항목")
    public record ScrumPreview(
            @Schema(description = "스크럼 식별자", example = "1") Long scrumId,
            @Schema(description = "프로젝트 태그명 (최대 15자)", example = "밋업프로젝트") String projectName,
            @Schema(description = "제목 자유작성 영역 (최대 20자)", example = "기획 작업") String freeText,
            @Schema(description = "스크럼 본문 (최대 50자)", example = "어드민 페이지 기능명세서 작성") String content,
            @Schema(
                            description = "대표 역량. STAR 완료 기록이 없으면 null",
                            example = "PLANNING_EXECUTION",
                            nullable = true)
                    CompetencyCategory primaryCategory,
            @Schema(
                            description = "세부 역량 태그 (최대 3개). STAR 완료 기록이 없으면 null",
                            example = "[\"UX 설계\", \"품질 관리\"]",
                            nullable = true)
                    List<String> detailTags,
            @Schema(description = "STAR 완료 여부", example = "true") boolean hasStar) {}
}
