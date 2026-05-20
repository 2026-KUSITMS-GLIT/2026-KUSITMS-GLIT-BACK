package com.groute.groute_server.calendar.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.calendar.repository.CalendarHomeRepository;
import com.groute.groute_server.calendar.repository.StarDailyRow;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;

import lombok.RequiredArgsConstructor;

/**
 * 캘린더 메인 홈 조회 서비스 (CAL-001).
 *
 * <p>월별 캘린더 데이터(잔디 색상용 대표 역량 + 스크럼/STAR 작성 표시)와 날짜 프리뷰를 조회한다. 본인 데이터만 반환하며 쓰기 로직은 갖지 않는다.
 *
 * <p>한 일자에 STAR가 여러 개 완료된 경우 대표 역량은 가장 최근 완료된 record의 {@code primaryCategory}를 사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarHomeService {

    private final CalendarHomeRepository calendarHomeRepository;

    /**
     * 월별 캘린더 데이터를 반환한다. 해당 월의 1일~말일 모든 날짜가 포함되며, 데이터가 없는 날도 빈 집계로 채운다.
     *
     * @param userId 조회 대상 사용자
     * @param month 조회 연월
     */
    public CalendarMonthlyView getMonthly(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        Set<LocalDate> scrumDates =
                new HashSet<>(calendarHomeRepository.findScrumDatesInRange(userId, start, end));

        Map<LocalDate, List<StarDailyRow>> starsByDate =
                calendarHomeRepository.findCompletedStarRowsInRange(userId, start, end).stream()
                        .collect(Collectors.groupingBy(StarDailyRow::scrumDate));

        List<CalendarMonthlyView.DayAggregate> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            boolean hasScrums = scrumDates.contains(d);
            List<StarDailyRow> dayStars = starsByDate.getOrDefault(d, List.of());
            int starCount =
                    (int) dayStars.stream().map(StarDailyRow::starRecordId).distinct().count();
            CompetencyCategory primary = pickPrimaryCategory(dayStars);
            days.add(
                    new CalendarMonthlyView.DayAggregate(
                            d, hasScrums, starCount > 0, primary, starCount));
        }
        return new CalendarMonthlyView(month, days);
    }

    /**
     * 지정 일자의 작업(ScrumTitle) 단위 프리뷰 목록을 반환한다. 본인 데이터만 포함되며 빈 결과는 {@code titles=[]}.
     *
     * <p>같은 freeText에 속한 스크럼들을 하나의 카드로 묶고, 카드 단위로 STAR 완료된 스크럼들의 {@code primaryCategory}를 distinct
     * 집합으로 노출한다. STAR 완료된 스크럼이 한 건도 없으면 {@code primaryCategories=[]}이고 {@code hasStarAny=false}다.
     * 정렬은 그룹 첫 발견 순서(scrum.id ASC) 를 따른다.
     */
    public CalendarDailyPreviewView getDailyPreview(Long userId, LocalDate date) {
        List<Scrum> scrums = calendarHomeRepository.findScrumsByUserAndDate(userId, date);
        if (scrums.isEmpty()) {
            return new CalendarDailyPreviewView(date, List.of());
        }

        List<Long> scrumIds = scrums.stream().map(Scrum::getId).toList();
        Map<Long, CompetencyCategory> primaryByScrumId =
                calendarHomeRepository.findCompletedStarTagsByScrumIds(userId, scrumIds).stream()
                        .collect(
                                Collectors.toMap(
                                        row -> row.scrumId(),
                                        row -> row.primaryCategory(),
                                        (a, b) -> a));

        // 카드 정렬 규칙(scrum.id ASC 첫 발견 순서)을 repository 쿼리 변경에 의존하지 않도록 서비스에서 명시적으로 정렬한다.
        Map<Long, List<Scrum>> scrumsByTitleId =
                scrums.stream()
                        .sorted(Comparator.comparing(Scrum::getId))
                        .collect(
                                Collectors.groupingBy(
                                        s -> s.getTitle().getId(),
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        List<CalendarDailyPreviewView.TitlePreview> titles = new ArrayList<>();
        for (List<Scrum> group : scrumsByTitleId.values()) {
            ScrumTitle title = group.get(0).getTitle();
            List<CompetencyCategory> primaryCategories =
                    group.stream()
                            .map(Scrum::getId)
                            .map(primaryByScrumId::get)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();
            titles.add(
                    new CalendarDailyPreviewView.TitlePreview(
                            title.getId(),
                            title.getProject().getName(),
                            title.getFreeText(),
                            primaryCategories,
                            group.size(),
                            !primaryCategories.isEmpty()));
        }
        return new CalendarDailyPreviewView(date, titles);
    }

    /** 그날 STAR 행 중 가장 최근 완료된 row의 primaryCategory. 비어 있으면 {@code null}. */
    private static CompetencyCategory pickPrimaryCategory(List<StarDailyRow> dayStars) {
        if (dayStars.isEmpty()) {
            return null;
        }
        return dayStars.stream()
                .max(
                        Comparator.comparing(
                                StarDailyRow::completedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(StarDailyRow::primaryCategory)
                .orElse(null);
    }
}
