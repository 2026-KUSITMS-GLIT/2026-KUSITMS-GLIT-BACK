package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.record.application.port.in.calendar.DailyCalendarView;
import com.groute.groute_server.record.application.port.in.calendar.GetDailyCalendarQuery;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.star.ScrumStarTagProjection;
import com.groute.groute_server.record.application.port.out.star.StarTagQueryPort;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.enums.CompetencyCategory;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class CalendarQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 5, 4);
    private static final GetDailyCalendarQuery QUERY = new GetDailyCalendarQuery(USER_ID, DATE);

    @Mock ScrumQueryPort scrumQueryPort;
    @Mock StarTagQueryPort starTagQueryPort;

    @InjectMocks CalendarQueryService service;

    @Nested
    @DisplayName("빈 일자")
    class Empty {

        @Test
        @DisplayName("스크럼이 없으면 빈 그룹·빈 detailTags를 반환한다")
        void should_returnEmptyGroupsAndTags_when_noScrumOnDate() {
            // given
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of());

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            assertThat(view.groups()).isEmpty();
            assertThat(view.detailTags()).isEmpty();
        }
    }

    @Nested
    @DisplayName("그룹핑·정렬")
    class GroupingAndOrder {

        @Test
        @DisplayName("동일 titleId의 스크럼들이 같은 그룹으로 묶이고 입력 순서가 보존된다")
        void should_groupByTitleAndPreserveInputOrder() {
            // given — 레포가 (titleId asc, id asc) 정렬해 반환한다고 가정
            ScrumTitle t1 = title(1L, "P1", "F1");
            ScrumTitle t2 = title(2L, "P2", "F2");
            Scrum s10 = scrum(10L, t1, "a", false, DATE.minusDays(1));
            Scrum s11 = scrum(11L, t1, "b", false, DATE.minusDays(1));
            Scrum s20 = scrum(20L, t2, "c", false, DATE.minusDays(1));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE))
                    .willReturn(List.of(s10, s11, s20));
            given(starTagQueryPort.findCompletedTagsByScrumIds(anyLong(), any()))
                    .willReturn(List.of());

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            assertThat(view.groups()).hasSize(2);
            DailyCalendarView.GroupView g1 = view.groups().get(0);
            assertThat(g1.titleId()).isEqualTo(1L);
            assertThat(g1.projectTag()).isEqualTo("P1");
            assertThat(g1.freeText()).isEqualTo("F1");
            assertThat(g1.items())
                    .extracting(DailyCalendarView.ItemView::scrumId)
                    .containsExactly(10L, 11L);
            DailyCalendarView.GroupView g2 = view.groups().get(1);
            assertThat(g2.titleId()).isEqualTo(2L);
            assertThat(g2.items())
                    .extracting(DailyCalendarView.ItemView::scrumId)
                    .containsExactly(20L);
        }
    }

    @Nested
    @DisplayName("isEditable 계산")
    class EditableComputation {

        @Test
        @DisplayName("13일 전 작성된 hasStar=false 스크럼은 isEditable=true")
        void should_beEditable_when_createdAt13DaysAgoAndNoStar() {
            // given
            ScrumTitle t = title(1L, "P", "F");
            Scrum scrum = scrum(10L, t, "x", false, LocalDate.now().minusDays(13));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(scrum));
            given(starTagQueryPort.findCompletedTagsByScrumIds(anyLong(), any()))
                    .willReturn(List.of());

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            assertThat(view.groups().get(0).items().get(0).isEditable()).isTrue();
        }

        @Test
        @DisplayName("15일 전 작성된 스크럼은 isEditable=false")
        void should_notBeEditable_when_createdAt15DaysAgo() {
            // given
            ScrumTitle t = title(1L, "P", "F");
            Scrum scrum = scrum(10L, t, "x", false, LocalDate.now().minusDays(15));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(scrum));
            given(starTagQueryPort.findCompletedTagsByScrumIds(anyLong(), any()))
                    .willReturn(List.of());

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            assertThat(view.groups().get(0).items().get(0).isEditable()).isFalse();
        }

        @Test
        @DisplayName("hasStar=true 스크럼은 14일 이내여도 isEditable=false")
        void should_notBeEditable_when_hasStar() {
            // given
            ScrumTitle t = title(1L, "P", "F");
            Scrum scrum = scrum(10L, t, "x", true, LocalDate.now().minusDays(1));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(scrum));
            given(starTagQueryPort.findCompletedTagsByScrumIds(anyLong(), any()))
                    .willReturn(List.of());

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            DailyCalendarView.ItemView item = view.groups().get(0).items().get(0);
            assertThat(item.hasStar()).isTrue();
            assertThat(item.isEditable()).isFalse();
        }
    }

    @Nested
    @DisplayName("group.isEditable 계산")
    class GroupEditable {

        @Test
        @DisplayName("그룹 내 한 item이라도 editable이면 group.isEditable=true")
        void should_beEditable_when_anyItemEditable() {
            // given — 같은 그룹: 하나는 hasStar(잠김), 하나는 정상
            ScrumTitle t = title(1L, "P", "F");
            Scrum locked = scrum(10L, t, "x", true, LocalDate.now().minusDays(1));
            Scrum open = scrum(11L, t, "y", false, LocalDate.now().minusDays(1));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE))
                    .willReturn(List.of(locked, open));
            given(starTagQueryPort.findCompletedTagsByScrumIds(anyLong(), any()))
                    .willReturn(List.of());

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            assertThat(view.groups().get(0).isEditable()).isTrue();
        }

        @Test
        @DisplayName("그룹 내 모든 item이 not editable이면 group.isEditable=false")
        void should_notBeEditable_when_allItemsLocked() {
            // given — 둘 다 hasStar
            ScrumTitle t = title(1L, "P", "F");
            Scrum a = scrum(10L, t, "x", true, LocalDate.now().minusDays(1));
            Scrum b = scrum(11L, t, "y", true, LocalDate.now().minusDays(1));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(a, b));
            given(starTagQueryPort.findCompletedTagsByScrumIds(anyLong(), any()))
                    .willReturn(List.of());

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            assertThat(view.groups().get(0).isEditable()).isFalse();
        }
    }

    @Nested
    @DisplayName("item primaryCategory·starRecordId·detailTags 집계")
    class TagAggregation {

        @Test
        @DisplayName("STAR 완료된 item에 primaryCategory와 starRecordId를 매핑한다")
        void should_setItemPrimaryCategoryAndStarRecordId_when_starCompleted() {
            // given — 한 카드 안에 2개 스크럼, 서로 다른 primaryCategory와 starRecordId
            ScrumTitle t = title(1L, "P", "F");
            Scrum a = scrum(10L, t, "x", true, LocalDate.now().minusDays(1));
            Scrum b = scrum(11L, t, "y", true, LocalDate.now().minusDays(1));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(a, b));
            given(starTagQueryPort.findCompletedTagsByScrumIds(anyLong(), any()))
                    .willReturn(
                            List.of(
                                    new ScrumStarTagProjection(
                                            10L,
                                            100L,
                                            CompetencyCategory.PLANNING_EXECUTION,
                                            "UX 설계"),
                                    new ScrumStarTagProjection(
                                            11L,
                                            101L,
                                            CompetencyCategory.COLLABORATION,
                                            "이해관계자 조율")));

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            List<DailyCalendarView.ItemView> items = view.groups().get(0).items();
            assertThat(items.get(0).primaryCategory())
                    .isEqualTo(CompetencyCategory.PLANNING_EXECUTION);
            assertThat(items.get(0).starRecordId()).isEqualTo(100L);
            assertThat(items.get(1).primaryCategory()).isEqualTo(CompetencyCategory.COLLABORATION);
            assertThat(items.get(1).starRecordId()).isEqualTo(101L);
        }

        @Test
        @DisplayName(
                "같은 scrum의 detailTag가 여러 row여도 primaryCategory·starRecordId는 첫 row 값으로 단일 매핑된다")
        void should_useFirstRowMapping_when_multipleDetailTagsPerScrum() {
            // given — scrumId=10에 detailTag가 2개 row
            ScrumTitle t = title(1L, "P", "F");
            Scrum s = scrum(10L, t, "x", true, LocalDate.now().minusDays(1));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(s));
            given(starTagQueryPort.findCompletedTagsByScrumIds(anyLong(), any()))
                    .willReturn(
                            List.of(
                                    new ScrumStarTagProjection(
                                            10L,
                                            100L,
                                            CompetencyCategory.PLANNING_EXECUTION,
                                            "UX 설계"),
                                    new ScrumStarTagProjection(
                                            10L,
                                            100L,
                                            CompetencyCategory.PLANNING_EXECUTION,
                                            "품질 관리")));

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            DailyCalendarView.ItemView item = view.groups().get(0).items().get(0);
            assertThat(item.primaryCategory()).isEqualTo(CompetencyCategory.PLANNING_EXECUTION);
            assertThat(item.starRecordId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("STAR 완료가 없는 item은 primaryCategory·starRecordId가 null이다")
        void should_returnNullPrimaryCategoryAndStarRecordId_when_noStarOnItem() {
            // given
            ScrumTitle t = title(1L, "P", "F");
            Scrum s = scrum(10L, t, "x", false, LocalDate.now().minusDays(1));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(s));
            given(starTagQueryPort.findCompletedTagsByScrumIds(anyLong(), any()))
                    .willReturn(List.of());

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            DailyCalendarView.ItemView item = view.groups().get(0).items().get(0);
            assertThat(item.primaryCategory()).isNull();
            assertThat(item.starRecordId()).isNull();
        }

        @Test
        @DisplayName("일자 전체 detailTag 합집합을 distinct·등장순서로 노출한다")
        void should_aggregateDetailTags_distinctAcrossCards() {
            // given — 서로 다른 카드의 스크럼이 같은 detailTag를 공유
            ScrumTitle t1 = title(1L, "P1", "F1");
            ScrumTitle t2 = title(2L, "P2", "F2");
            Scrum s10 = scrum(10L, t1, "a", true, LocalDate.now().minusDays(1));
            Scrum s20 = scrum(20L, t2, "b", true, LocalDate.now().minusDays(1));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(s10, s20));
            given(starTagQueryPort.findCompletedTagsByScrumIds(anyLong(), any()))
                    .willReturn(
                            List.of(
                                    new ScrumStarTagProjection(
                                            10L,
                                            100L,
                                            CompetencyCategory.PLANNING_EXECUTION,
                                            "UX 설계"),
                                    new ScrumStarTagProjection(
                                            10L,
                                            100L,
                                            CompetencyCategory.PLANNING_EXECUTION,
                                            "품질 관리"),
                                    new ScrumStarTagProjection(
                                            20L, 200L, CompetencyCategory.COLLABORATION, "UX 설계"),
                                    new ScrumStarTagProjection(
                                            20L, 200L, CompetencyCategory.COLLABORATION, "고객 지원")));

            // when
            DailyCalendarView view = service.getDailyCalendar(QUERY);

            // then
            assertThat(view.detailTags()).containsExactly("UX 설계", "품질 관리", "고객 지원");
        }
    }

    // ============== helpers ==============

    private static ScrumTitle title(Long id, String projectName, String freeText) {
        Project project = Project.builder().id(1000L + id).name(projectName).build();
        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", id);
        ReflectionTestUtils.setField(title, "project", project);
        ReflectionTestUtils.setField(title, "freeText", freeText);
        return title;
    }

    private static Scrum scrum(
            Long id,
            ScrumTitle title,
            String content,
            boolean hasStar,
            LocalDate createdLocalDate) {
        Scrum scrum = Scrum.create(User.createForSocialLogin(), title, content, DATE);
        ReflectionTestUtils.setField(scrum, "id", id);
        ReflectionTestUtils.setField(scrum, "hasStar", hasStar);
        OffsetDateTime createdAt =
                createdLocalDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        ReflectionTestUtils.setField(scrum, "createdAt", createdAt);
        return scrum;
    }
}
