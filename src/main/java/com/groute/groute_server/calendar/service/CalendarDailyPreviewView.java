package com.groute.groute_server.calendar.service;

import java.time.LocalDate;
import java.util.List;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

/**
 * 날짜 프리뷰 도메인 뷰.
 *
 * <p>지정 일자의 작업(ScrumTitle) 단위 프리뷰 목록을 service ↔ controller 사이에 전달한다. 같은 freeText에 속한 스크럼들을 하나의 카드로
 * 묶고, 카드 단위로 STAR 완료된 스크럼들의 {@code primaryCategory}를 distinct 집합으로 노출한다.
 */
public record CalendarDailyPreviewView(LocalDate date, List<TitlePreview> titles) {

    /**
     * 작업(ScrumTitle) 단위 프리뷰 항목.
     *
     * <p>{@code primaryCategories}는 카드 내 STAR 완료된 스크럼들의 distinct primaryCategory 집합으로, scrum.id 오름차순
     * 첫 발견 순서를 따른다. STAR 완료된 스크럼이 한 건도 없으면 빈 배열이고 {@code hasStarAny}는 false다.
     */
    public record TitlePreview(
            Long titleId,
            String projectName,
            String freeText,
            List<CompetencyCategory> primaryCategories,
            int scrumCount,
            boolean hasStarAny) {}
}
