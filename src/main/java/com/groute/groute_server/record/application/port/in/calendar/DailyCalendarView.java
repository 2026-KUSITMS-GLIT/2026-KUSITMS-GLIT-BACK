package com.groute.groute_server.record.application.port.in.calendar;

import java.util.List;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

/**
 * 일자별 스크럼 조회·sync 응답 모델.
 *
 * <p>ScrumTitle 단위로 그룹핑된 2계층 구조. {@code GetDailyCalendarUseCase}와 {@code SyncDailyScrumUseCase}가 동일
 * schema를 반환한다.
 *
 * @param detailTags 해당 일자의 모든 STAR에서 추출된 distinct 세부역량태그 목록. STAR가 한 건도 없으면 빈 배열.
 */
public record DailyCalendarView(List<String> detailTags, List<GroupView> groups) {

    /**
     * ScrumTitle 단위 그룹.
     *
     * @param isEditable 그룹 내 수정 가능한 item이 하나라도 있는지 (UI 그룹 X 버튼 노출 기준)
     */
    public record GroupView(
            Long titleId,
            String projectTag,
            String freeText,
            boolean isEditable,
            List<ItemView> items) {}

    /**
     * Scrum 단위 항목.
     *
     * @param primaryCategory STAR 완료 시 대표 역량(막대기 색상 결정), 아니면 null
     * @param starRecordId hasStar=true인 경우 연결된 STAR id(상세 페이지 이동용), 아니면 null
     * @param isEditable 작성 14일 이내이고 hasStar=false 일 때 true
     */
    public record ItemView(
            Long scrumId,
            String content,
            boolean hasStar,
            CompetencyCategory primaryCategory,
            Long starRecordId,
            boolean isEditable) {}
}
