package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.scrum.SyncDailyScrumCommand;
import com.groute.groute_server.record.application.port.in.scrum.SyncDailyScrumCommand.GroupCommand;
import com.groute.groute_server.record.application.port.in.scrum.SyncDailyScrumCommand.ItemCommand;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordCascadePort;
import com.groute.groute_server.record.application.port.out.user.UserReferencePort;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class ScrumSyncServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 5, 4);

    @Mock ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    @Mock ScrumQueryPort scrumQueryPort;
    @Mock ScrumWritePort scrumWritePort;
    @Mock StarRecordCascadePort starRecordCascadePort;
    @Mock UserReferencePort userReferencePort;

    @InjectMocks ScrumSyncService service;

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("일자당 6개를 보내면 SCRUM_DATE_LIMIT_EXCEEDED를 던진다")
        void should_throwDateLimitExceeded_when_moreThan5Items() {
            // given
            SyncDailyScrumCommand command =
                    command(
                            group(
                                    1L,
                                    item(null, "a"),
                                    item(null, "b"),
                                    item(null, "c"),
                                    item(null, "d"),
                                    item(null, "e"),
                                    item(null, "f")));

            // when & then
            assertThatThrownBy(() -> service.syncDailyScrum(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_DATE_LIMIT_EXCEEDED);
            verify(scrumWritePort, never()).saveAll(anyCollection());
        }

        @Test
        @DisplayName("titleId 일부가 본인 소유가 아니면 TITLE_NOT_FOUND를 던진다")
        void should_throwTitleNotFound_when_anyTitleNotOwned() {
            // given — 요청은 title 1L, 2L이지만 1L만 본인 소유
            ScrumTitle owned = title(1L, "P", "F");
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(owned));
            SyncDailyScrumCommand command =
                    command(group(1L, item(null, "x")), group(2L, item(null, "y")));

            // when & then
            assertThatThrownBy(() -> service.syncDailyScrum(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TITLE_NOT_FOUND);
            verify(scrumWritePort, never()).saveAll(anyCollection());
        }

        @Test
        @DisplayName("scrumId 일부가 본인 소유가 아니면 SCRUM_NOT_FOUND를 던진다")
        void should_throwScrumNotFound_when_anyScrumNotOwned() {
            // given — 요청 scrumId 10L, 11L 중 10L만 본인 소유
            ScrumTitle title = title(1L, "P", "F");
            Scrum owned = scrum(10L, title, "old", false, DATE.minusDays(1));
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(owned));
            SyncDailyScrumCommand command = command(group(1L, item(10L, "old"), item(11L, "old2")));

            // when & then
            assertThatThrownBy(() -> service.syncDailyScrum(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_NOT_FOUND);
            verify(scrumWritePort, never()).saveAll(anyCollection());
        }

        @Test
        @DisplayName("scrumId가 다른 일자의 것이면 SCRUM_NOT_FOUND를 던진다")
        void should_throwScrumNotFound_when_scrumOnOtherDate() {
            // given — scrumId는 본인 소유지만 currentScrums(=DATE)에는 없음
            ScrumTitle title = title(1L, "P", "F");
            Scrum otherDateScrum = scrum(10L, title, "x", false, DATE.minusDays(1));
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(otherDateScrum));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of());
            SyncDailyScrumCommand command = command(group(1L, item(10L, "x")));

            // when & then
            assertThatThrownBy(() -> service.syncDailyScrum(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("신규 item만 보내면 saveAll 호출과 카운터 +1을 수행한다")
        void should_saveAllAndIncrementCounter_when_onlyNewItems() {
            // given
            ScrumTitle title = title(1L, "P", "F");
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of());
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of());
            SyncDailyScrumCommand command = command(group(1L, item(null, "신규")));

            // when
            service.syncDailyScrum(command);

            // then
            ArgumentCaptor<Collection<Scrum>> captor = scrumCollectionCaptor();
            verify(scrumWritePort).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            verify(scrumTitleRepositoryPort).applyScrumCountIncrement(1L, 1);
            verify(scrumWritePort, never()).updateContent(anyLong(), any());
            verify(scrumWritePort, never()).softDeleteAllByIdIn(anyCollection());
            verify(starRecordCascadePort, never()).cascadeDeleteByScrumIdIn(anyCollection());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("기존 item content가 바뀌면 updateContent를 호출한다")
        void should_callUpdateContent_when_contentChanged() {
            // given — DB의 scrum 10 content "old", 요청 "new"
            ScrumTitle title = title(1L, "P", "F");
            Scrum existing = scrum(10L, title, "old", false, DATE.minusDays(1));
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(existing));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(existing));
            SyncDailyScrumCommand command = command(group(1L, item(10L, "new")));

            // when
            service.syncDailyScrum(command);

            // then
            verify(scrumWritePort).updateContent(10L, "new");
            verify(scrumTitleRepositoryPort, never()).applyScrumCountIncrement(anyLong(), anyInt());
            verify(scrumWritePort, never()).softDeleteAllByIdIn(anyCollection());
        }

        @Test
        @DisplayName("기존 item content가 동일하면 updateContent를 호출하지 않는다")
        void should_notCallUpdateContent_when_contentUnchanged() {
            // given
            ScrumTitle title = title(1L, "P", "F");
            Scrum existing = scrum(10L, title, "same", false, DATE.minusDays(1));
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(existing));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(existing));
            SyncDailyScrumCommand command = command(group(1L, item(10L, "same")));

            // when
            service.syncDailyScrum(command);

            // then
            verify(scrumWritePort, never()).updateContent(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("요청에서 빠진 기존 item은 삭제 + STAR cascade + 카운터 -1을 수행한다")
        void should_softDeleteAndCascadeAndDecrement_when_itemMissingFromRequest() {
            // given — DB에 scrum 10 있음, 요청은 빈 items
            ScrumTitle title = title(1L, "P", "F");
            Scrum existing = scrum(10L, title, "x", false, DATE.minusDays(1));
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of());
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(existing));
            SyncDailyScrumCommand command = command(group(1L));

            // when
            service.syncDailyScrum(command);

            // then
            verify(scrumWritePort).softDeleteAllByIdIn(Set.of(10L));
            verify(starRecordCascadePort).cascadeDeleteByScrumIdIn(Set.of(10L));
            verify(scrumTitleRepositoryPort).applyScrumCountIncrement(1L, -1);
        }
    }

    @Nested
    @DisplayName("14일 경계")
    class FourteenDayBoundary {

        @Test
        @DisplayName("13일 전 작성된 스크럼은 수정 가능하다")
        void should_allowUpdate_when_createdAt13DaysAgo() {
            // given
            ScrumTitle title = title(1L, "P", "F");
            Scrum existing = scrum(10L, title, "old", false, LocalDate.now().minusDays(13));
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(existing));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(existing));
            SyncDailyScrumCommand command = command(group(1L, item(10L, "new")));

            // when & then
            assertThatCode(() -> service.syncDailyScrum(command)).doesNotThrowAnyException();
            verify(scrumWritePort).updateContent(10L, "new");
        }

        @Test
        @DisplayName("15일 전 작성된 스크럼 수정 시도는 SCRUM_EDIT_LOCKED_14D를 던진다")
        void should_throwLocked14d_when_createdAt15DaysAgo() {
            // given
            ScrumTitle title = title(1L, "P", "F");
            Scrum existing = scrum(10L, title, "old", false, LocalDate.now().minusDays(15));
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(existing));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(existing));
            SyncDailyScrumCommand command = command(group(1L, item(10L, "new")));

            // when & then
            assertThatThrownBy(() -> service.syncDailyScrum(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_EDIT_LOCKED_14D);
        }
    }

    @Nested
    @DisplayName("STAR 잠금")
    class StarLock {

        @Test
        @DisplayName("hasStar=true 스크럼 삭제 시도는 SCRUM_EDIT_LOCKED_STAR를 던진다")
        void should_throwLockedStar_when_deletingHasStarScrum() {
            // given — DB에 hasStar 스크럼 1개, 요청에서 빠짐
            ScrumTitle title = title(1L, "P", "F");
            Scrum starred = scrum(10L, title, "x", true, DATE.minusDays(1));
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of());
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(starred));
            SyncDailyScrumCommand command = command(group(1L));

            // when & then
            assertThatThrownBy(() -> service.syncDailyScrum(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_EDIT_LOCKED_STAR);
            verify(scrumWritePort, never()).softDeleteAllByIdIn(anyCollection());
            verify(starRecordCascadePort, never()).cascadeDeleteByScrumIdIn(anyCollection());
        }
    }

    @Nested
    @DisplayName("mixed")
    class Mixed {

        @Test
        @DisplayName("추가+수정+삭제가 섞이면 각각 정확히 호출되고 카운터도 그룹별로 갱신된다")
        void should_applyAllChangesAndCounters_when_mixed() {
            // given
            ScrumTitle t1 = title(1L, "P1", "F1");
            ScrumTitle t2 = title(2L, "P2", "F2");
            Scrum a = scrum(100L, t1, "A_old", false, DATE.minusDays(1)); // 수정 대상
            Scrum b = scrum(101L, t1, "B", false, DATE.minusDays(1)); // 삭제 대상
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(t1, t2));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(a));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(a, b));
            SyncDailyScrumCommand command =
                    command(
                            group(1L, item(100L, "A_new")),
                            group(2L, item(null, "C"), item(null, "D")));

            // when
            service.syncDailyScrum(command);

            // then
            verify(scrumWritePort).updateContent(100L, "A_new");
            verify(scrumWritePort).softDeleteAllByIdIn(Set.of(101L));
            verify(starRecordCascadePort).cascadeDeleteByScrumIdIn(Set.of(101L));
            ArgumentCaptor<Collection<Scrum>> captor = scrumCollectionCaptor();
            verify(scrumWritePort).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
            verify(scrumTitleRepositoryPort).applyScrumCountIncrement(2L, 2);
            verify(scrumTitleRepositoryPort).applyScrumCountIncrement(1L, -1);
        }
    }

    @Nested
    @DisplayName("rollback 시나리오")
    class Rollback {

        @Test
        @DisplayName("14일 위반과 신규 추가가 섞여 있으면 어떤 쓰기도 수행하지 않는다")
        void should_callNoWrites_when_validationFailsBeforeApply() {
            // given — A는 15일 전(잠김) 수정 대상, B는 신규
            ScrumTitle title = title(1L, "P", "F");
            Scrum locked = scrum(100L, title, "old", false, LocalDate.now().minusDays(15));
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByIdInAndUserId(anyCollection(), anyLong()))
                    .willReturn(List.of(locked));
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE)).willReturn(List.of(locked));
            SyncDailyScrumCommand command = command(group(1L, item(100L, "new"), item(null, "B")));

            // when & then
            assertThatThrownBy(() -> service.syncDailyScrum(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_EDIT_LOCKED_14D);
            verify(scrumWritePort, never()).saveAll(anyCollection());
            verify(scrumWritePort, never()).updateContent(anyLong(), any());
            verify(scrumWritePort, never()).softDeleteAllByIdIn(anyCollection());
            verify(starRecordCascadePort, never()).cascadeDeleteByScrumIdIn(anyCollection());
            verify(scrumTitleRepositoryPort, never()).applyScrumCountIncrement(anyLong(), anyInt());
        }
    }

    // ============== helpers ==============

    private static SyncDailyScrumCommand command(GroupCommand... groups) {
        return new SyncDailyScrumCommand(USER_ID, DATE, List.of(groups));
    }

    private static GroupCommand group(Long titleId, ItemCommand... items) {
        return new GroupCommand(titleId, List.of(items));
    }

    private static ItemCommand item(Long scrumId, String content) {
        return new ItemCommand(scrumId, content);
    }

    private static ScrumTitle title(Long id, String projectName, String freeText) {
        Project project = new Project();
        ReflectionTestUtils.setField(project, "id", 1000L + id);
        ReflectionTestUtils.setField(project, "name", projectName);
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

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Collection<Scrum>> scrumCollectionCaptor() {
        return ArgumentCaptor.forClass(Collection.class);
    }
}
