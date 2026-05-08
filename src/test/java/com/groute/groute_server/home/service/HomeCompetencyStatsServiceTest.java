package com.groute.groute_server.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.home.repository.CompetencyStatsQueryRepository;
import com.groute.groute_server.home.repository.CompetencyStatsQueryRepository.DateCountRow;

@ExtendWith(MockitoExtension.class)
class HomeCompetencyStatsServiceTest {

    private static final Long USER_ID = 1L;

    @Mock CompetencyStatsQueryRepository competencyStatsQueryRepository;

    @InjectMocks HomeCompetencyStatsService homeCompetencyStatsService;

    @Nested
    @DisplayName("월별 STAR 완료 건수 집계 - 정상")
    class HappyPath {

        @Test
        @DisplayName("YearMonth를 [월 첫날, 다음달 첫날) 반열림 구간으로 변환해 repository에 위임한다")
        void should_callRepoWithHalfOpenMonthRange_when_givenYearMonth() {
            // given
            YearMonth month = YearMonth.of(2026, 4);

            // when
            homeCompetencyStatsService.getCompletedStarCountsByMonth(USER_ID, month);

            // then
            verify(competencyStatsQueryRepository)
                    .findCompletedStarCountsByUserAndDateRange(
                            USER_ID, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1));
        }

        @Test
        @DisplayName("repository 결과 행을 일자→건수 맵으로 수집한다")
        void should_collectRowsToDateCountMap_when_repoReturnsMultipleRows() {
            // given
            YearMonth month = YearMonth.of(2026, 4);
            // 행 mock의 stubbing이 outer given(...) 안에서 평가되면 Mockito가
            // UnfinishedStubbingException을 던지므로, 행을 먼저 빌드한 뒤 outer를 stubbing한다.
            List<DateCountRow> rows =
                    List.of(
                            mockRow(LocalDate.of(2026, 4, 3), 1L),
                            mockRow(LocalDate.of(2026, 4, 7), 2L),
                            mockRow(LocalDate.of(2026, 4, 15), 5L));
            given(
                            competencyStatsQueryRepository
                                    .findCompletedStarCountsByUserAndDateRange(
                                            USER_ID,
                                            LocalDate.of(2026, 4, 1),
                                            LocalDate.of(2026, 5, 1)))
                    .willReturn(rows);

            // when
            Map<LocalDate, Long> result =
                    homeCompetencyStatsService.getCompletedStarCountsByMonth(USER_ID, month);

            // then
            assertThat(result)
                    .containsOnly(
                            Map.entry(LocalDate.of(2026, 4, 3), 1L),
                            Map.entry(LocalDate.of(2026, 4, 7), 2L),
                            Map.entry(LocalDate.of(2026, 4, 15), 5L));
        }

        @Test
        @DisplayName("repository 결과가 비어 있으면 빈 맵을 반환한다 (NO_DATA 채움은 컨트롤러 책임)")
        void should_returnEmptyMap_when_repoReturnsNoRows() {
            // given
            YearMonth month = YearMonth.of(2026, 4);

            // when
            Map<LocalDate, Long> result =
                    homeCompetencyStatsService.getCompletedStarCountsByMonth(USER_ID, month);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("윤년 2월은 endExclusive=3/1로 변환되어 29일을 포함한다")
        void should_resolveLeapFebruaryEndAsMarchFirst_when_given2024Feb() {
            // given
            YearMonth month = YearMonth.of(2024, 2);

            // when
            homeCompetencyStatsService.getCompletedStarCountsByMonth(USER_ID, month);

            // then
            verify(competencyStatsQueryRepository)
                    .findCompletedStarCountsByUserAndDateRange(
                            USER_ID, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1));
        }

        @Test
        @DisplayName("12월은 endExclusive를 다음 해 1/1로 변환해 연 경계를 안전하게 넘는다")
        void should_resolveDecemberEndAsNextYearJanuaryFirst_when_givenDecember() {
            // given
            YearMonth month = YearMonth.of(2026, 12);

            // when
            homeCompetencyStatsService.getCompletedStarCountsByMonth(USER_ID, month);

            // then
            verify(competencyStatsQueryRepository)
                    .findCompletedStarCountsByUserAndDateRange(
                            USER_ID, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1));
        }
    }

    private static DateCountRow mockRow(LocalDate date, long count) {
        DateCountRow row = mock(DateCountRow.class);
        given(row.getDate()).willReturn(date);
        given(row.getCount()).willReturn(count);
        return row;
    }
}
