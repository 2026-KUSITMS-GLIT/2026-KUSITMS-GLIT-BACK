package com.groute.groute_server.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.calendar.repository.CalendarHomeRepository;
import com.groute.groute_server.calendar.repository.ScrumStarTagRow;
import com.groute.groute_server.calendar.repository.StarDailyRow;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;

@ExtendWith(MockitoExtension.class)
class CalendarHomeServiceTest {

    private static final Long USER_ID = 1L;
    private static final YearMonth MONTH = YearMonth.of(2026, 4);
    private static final LocalDate DATE = LocalDate.of(2026, 4, 2);

    @Mock CalendarHomeRepository calendarHomeRepository;

    @InjectMocks CalendarHomeService service;

    @Nested
    @DisplayName("getMonthly - HappyPath")
    class MonthlyHappyPath {

        @Test
        @DisplayName("스크럼·STAR가 일부 일자에만 있을 때 1일~말일 모두 채우고 빈 날은 빈 집계로 표현한다")
        void should_fillAllDays_when_someScrumDatesAndStars() {
            // given
            LocalDate start = MONTH.atDay(1);
            LocalDate end = MONTH.atEndOfMonth();
            LocalDate scrumOnly = LocalDate.of(2026, 4, 1);
            LocalDate scrumWithStar = LocalDate.of(2026, 4, 2);
            given(calendarHomeRepository.findScrumDatesInRange(USER_ID, start, end))
                    .willReturn(List.of(scrumOnly, scrumWithStar));
            given(calendarHomeRepository.findCompletedStarRowsInRange(USER_ID, start, end))
                    .willReturn(
                            List.of(
                                    starRow(
                                            10L,
                                            scrumWithStar,
                                            OffsetDateTime.parse("2026-04-02T10:00:00Z"),
                                            CompetencyCategory.PLANNING_EXECUTION),
                                    // 같은 starRecordId의 두 번째 StarTag row → distinct로 1개로 카운트
                                    starRow(
                                            10L,
                                            scrumWithStar,
                                            OffsetDateTime.parse("2026-04-02T10:00:00Z"),
                                            CompetencyCategory.PLANNING_EXECUTION)));

            // when
            CalendarMonthlyView view = service.getMonthly(USER_ID, MONTH);

            // then
            assertThat(view.month()).isEqualTo(MONTH);
            assertThat(view.days()).hasSize(end.getDayOfMonth());

            CalendarMonthlyView.DayAggregate april1 = view.days().get(0);
            assertThat(april1.date()).isEqualTo(scrumOnly);
            assertThat(april1.hasScrums()).isTrue();
            assertThat(april1.hasStar()).isFalse();
            assertThat(april1.primaryCategory()).isNull();
            assertThat(april1.starCount()).isZero();

            CalendarMonthlyView.DayAggregate april2 = view.days().get(1);
            assertThat(april2.hasScrums()).isTrue();
            assertThat(april2.hasStar()).isTrue();
            assertThat(april2.primaryCategory()).isEqualTo(CompetencyCategory.PLANNING_EXECUTION);
            assertThat(april2.starCount()).isEqualTo(1);

            CalendarMonthlyView.DayAggregate april3 = view.days().get(2);
            assertThat(april3.hasScrums()).isFalse();
            assertThat(april3.hasStar()).isFalse();
            assertThat(april3.primaryCategory()).isNull();
            assertThat(april3.starCount()).isZero();
        }

        @Test
        @DisplayName("같은 날 여러 STAR가 완료되면 가장 최근 완료 record의 primaryCategory를 사용한다")
        void should_pickLatestPrimaryCategory_when_multipleStarsOnSameDay() {
            // given
            LocalDate start = MONTH.atDay(1);
            LocalDate end = MONTH.atEndOfMonth();
            LocalDate day = LocalDate.of(2026, 4, 5);
            given(calendarHomeRepository.findScrumDatesInRange(USER_ID, start, end))
                    .willReturn(List.of(day));
            given(calendarHomeRepository.findCompletedStarRowsInRange(USER_ID, start, end))
                    .willReturn(
                            List.of(
                                    starRow(
                                            1L,
                                            day,
                                            OffsetDateTime.parse("2026-04-05T08:00:00Z"),
                                            CompetencyCategory.COLLABORATION),
                                    starRow(
                                            2L,
                                            day,
                                            OffsetDateTime.parse("2026-04-05T15:00:00Z"),
                                            CompetencyCategory.PLANNING_EXECUTION)));

            // when
            CalendarMonthlyView view = service.getMonthly(USER_ID, MONTH);

            // then
            CalendarMonthlyView.DayAggregate april5 = view.days().get(4);
            assertThat(april5.starCount()).isEqualTo(2);
            assertThat(april5.primaryCategory()).isEqualTo(CompetencyCategory.PLANNING_EXECUTION);
        }

        @Test
        @DisplayName("데이터가 없는 월이면 모든 일자가 빈 집계로 채워진다")
        void should_returnEmptyAggregates_when_noData() {
            // given
            LocalDate start = MONTH.atDay(1);
            LocalDate end = MONTH.atEndOfMonth();
            given(calendarHomeRepository.findScrumDatesInRange(USER_ID, start, end))
                    .willReturn(List.of());
            given(calendarHomeRepository.findCompletedStarRowsInRange(USER_ID, start, end))
                    .willReturn(List.of());

            // when
            CalendarMonthlyView view = service.getMonthly(USER_ID, MONTH);

            // then
            assertThat(view.days()).hasSize(30);
            assertThat(view.days())
                    .allSatisfy(
                            d -> {
                                assertThat(d.hasScrums()).isFalse();
                                assertThat(d.hasStar()).isFalse();
                                assertThat(d.primaryCategory()).isNull();
                                assertThat(d.starCount()).isZero();
                            });
        }
    }

    @Nested
    @DisplayName("getDailyPreview - HappyPath")
    class DailyPreviewHappyPath {

        @Test
        @DisplayName("STAR가 완료된 스크럼은 primaryCategory와 detailTags가 채워진다")
        void should_fillStarFields_when_starCompleted() {
            // given
            Scrum scrum = scrum(7L, "어드민 페이지 기능명세서 작성", true, 50L, "밋업프로젝트", "기획 작업");
            given(calendarHomeRepository.findScrumsByUserAndDate(USER_ID, DATE))
                    .willReturn(List.of(scrum));
            given(calendarHomeRepository.findCompletedStarTagsByScrumIds(USER_ID, List.of(7L)))
                    .willReturn(
                            List.of(
                                    new ScrumStarTagRow(
                                            7L, CompetencyCategory.PLANNING_EXECUTION, "UX 설계"),
                                    new ScrumStarTagRow(
                                            7L, CompetencyCategory.PLANNING_EXECUTION, "품질 관리")));

            // when
            CalendarDailyPreviewView view = service.getDailyPreview(USER_ID, DATE);

            // then
            assertThat(view.date()).isEqualTo(DATE);
            assertThat(view.scrums()).hasSize(1);
            CalendarDailyPreviewView.ScrumItem item = view.scrums().get(0);
            assertThat(item.scrumId()).isEqualTo(7L);
            assertThat(item.projectName()).isEqualTo("밋업프로젝트");
            assertThat(item.freeText()).isEqualTo("기획 작업");
            assertThat(item.content()).isEqualTo("어드민 페이지 기능명세서 작성");
            assertThat(item.primaryCategory()).isEqualTo(CompetencyCategory.PLANNING_EXECUTION);
            assertThat(item.detailTags()).containsExactly("UX 설계", "품질 관리");
            assertThat(item.hasStar()).isTrue();
        }

        @Test
        @DisplayName("STAR가 미완료/미작성인 스크럼은 primaryCategory와 detailTags가 null")
        void should_returnNullStarFields_when_noCompletedStar() {
            // given — 스크럼은 있으나 완료된 STAR row 없음
            Scrum scrum = scrum(7L, "본문", false, 50L, "밋업프로젝트", "기획 작업");
            given(calendarHomeRepository.findScrumsByUserAndDate(USER_ID, DATE))
                    .willReturn(List.of(scrum));
            given(calendarHomeRepository.findCompletedStarTagsByScrumIds(USER_ID, List.of(7L)))
                    .willReturn(List.of());

            // when
            CalendarDailyPreviewView view = service.getDailyPreview(USER_ID, DATE);

            // then
            CalendarDailyPreviewView.ScrumItem item = view.scrums().get(0);
            assertThat(item.primaryCategory()).isNull();
            assertThat(item.detailTags()).isNull();
            assertThat(item.hasStar()).isFalse();
        }

        @Test
        @DisplayName("스크럼이 없으면 빈 배열을 반환한다")
        void should_returnEmptyScrums_when_noScrums() {
            // given
            given(calendarHomeRepository.findScrumsByUserAndDate(USER_ID, DATE))
                    .willReturn(List.of());

            // when
            CalendarDailyPreviewView view = service.getDailyPreview(USER_ID, DATE);

            // then
            assertThat(view.scrums()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Errors / 격리")
    class Isolation {

        @Test
        @DisplayName("repository에 호출 user의 USER_ID를 그대로 전달한다 (다른 user 데이터 누출 방지)")
        void should_passUserId_when_query() {
            // given
            given(calendarHomeRepository.findScrumsByUserAndDate(USER_ID, DATE))
                    .willReturn(List.of());

            // when
            service.getDailyPreview(USER_ID, DATE);

            // then
            verify(calendarHomeRepository).findScrumsByUserAndDate(eq(USER_ID), eq(DATE));
        }
    }

    // ============== helpers ==============

    private static StarDailyRow starRow(
            Long starRecordId,
            LocalDate scrumDate,
            OffsetDateTime completedAt,
            CompetencyCategory primary) {
        return new StarDailyRow(starRecordId, scrumDate, completedAt, primary);
    }

    private static Scrum scrum(
            Long id,
            String content,
            boolean hasStar,
            Long titleId,
            String projectName,
            String freeText) {
        Scrum scrum = new Scrum();
        ReflectionTestUtils.setField(scrum, "id", id);
        ReflectionTestUtils.setField(scrum, "content", content);
        ReflectionTestUtils.setField(scrum, "hasStar", hasStar);
        ReflectionTestUtils.setField(scrum, "title", title(titleId, projectName, freeText));
        return scrum;
    }

    private static ScrumTitle title(Long id, String projectName, String freeText) {
        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", id);
        ReflectionTestUtils.setField(title, "project", project(1L, projectName));
        ReflectionTestUtils.setField(title, "freeText", freeText);
        return title;
    }

    private static Project project(Long id, String name) {
        // Project no-args ctor가 PROTECTED라 reflection으로 인스턴스화
        try {
            Constructor<Project> ctor = Project.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Project project = ctor.newInstance();
            ReflectionTestUtils.setField(project, "id", id);
            ReflectionTestUtils.setField(project, "name", name);
            return project;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
