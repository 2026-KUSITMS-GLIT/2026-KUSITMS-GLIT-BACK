package com.groute.groute_server.calendar.service;

import java.time.LocalDate;
import java.util.List;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

/**
 * 날짜 프리뷰 도메인 뷰.
 *
 * <p>지정 일자의 스크럼 목록을 service ↔ controller 사이에 전달한다. STAR 완료 기록이 있는 스크럼만 {@code
 * primaryCategory}/{@code detailTags}가 채워지며, 미완료/미작성이면 두 필드 모두 {@code null}.
 */
public record CalendarDailyPreviewView(LocalDate date, List<ScrumItem> scrums) {

    public record ScrumItem(
            Long scrumId,
            String projectName,
            String freeText,
            String content,
            CompetencyCategory primaryCategory,
            List<String> detailTags,
            boolean hasStar) {}
}
