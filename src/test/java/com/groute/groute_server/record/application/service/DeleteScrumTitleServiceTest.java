package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.scrumtitle.DeleteScrumTitleCommand;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordCascadePort;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class DeleteScrumTitleServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TITLE_ID = 100L;
    private static final Set<Long> TITLE_IDS = Set.of(TITLE_ID);

    @Mock ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    @Mock ScrumQueryPort scrumQueryPort;
    @Mock ScrumWritePort scrumWritePort;
    @Mock StarRecordCascadePort starRecordCascadePort;
    @Mock StarImageCascadeCleaner starImageCascadeCleaner;

    @InjectMocks DeleteScrumTitleService service;

    @Nested
    @DisplayName("정상 삭제")
    class HappyPath {

        @Test
        @DisplayName(
                "산하 Scrum이 있으면 이미지 cleanup·scrum soft-delete·STAR cascade·title soft-delete를 모두 호출한다")
        void should_softDeleteAllScrums_andTitle_when_titleHasScrums() {
            // given
            ScrumTitle title = title(TITLE_ID);
            Scrum s1 = scrum(10L, title, false);
            Scrum s2 = scrum(11L, title, false);
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(TITLE_IDS, USER_ID))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByTitleIdAndUserId(TITLE_ID, USER_ID))
                    .willReturn(List.of(s1, s2));

            // when
            assertThatCode(
                            () ->
                                    service.deleteScrumTitle(
                                            new DeleteScrumTitleCommand(USER_ID, TITLE_ID)))
                    .doesNotThrowAnyException();

            // then
            Set<Long> expectedScrumIds = Set.of(10L, 11L);
            verify(starImageCascadeCleaner).cleanupByScrumIds(expectedScrumIds);
            verify(scrumWritePort).softDeleteAllByIdIn(expectedScrumIds);
            verify(starRecordCascadePort).cascadeDeleteByScrumIdIn(expectedScrumIds);
            verify(scrumTitleRepositoryPort).softDeleteAllByIds(List.of(TITLE_ID));
        }

        @Test
        @DisplayName("산하 Scrum이 0개면 cleanup·soft-delete·cascade는 호출하지 않고 title만 삭제한다")
        void should_softDeleteTitleOnly_when_titleHasNoScrums() {
            // given
            ScrumTitle title = title(TITLE_ID);
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(TITLE_IDS, USER_ID))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByTitleIdAndUserId(TITLE_ID, USER_ID))
                    .willReturn(List.of());

            // when
            service.deleteScrumTitle(new DeleteScrumTitleCommand(USER_ID, TITLE_ID));

            // then
            verify(starImageCascadeCleaner, never()).cleanupByScrumIds(anyCollection());
            verify(scrumWritePort, never()).softDeleteAllByIdIn(anyCollection());
            verify(starRecordCascadePort, never()).cascadeDeleteByScrumIdIn(anyCollection());
            verify(scrumTitleRepositoryPort).softDeleteAllByIds(List.of(TITLE_ID));
        }

        @Test
        @DisplayName("산하에 hasStar=true 스크럼이 있어도 동일하게 STAR cascade를 호출한다")
        void should_cascadeStar_when_anyScrumHasStar() {
            // given
            ScrumTitle title = title(TITLE_ID);
            Scrum starred = scrum(10L, title, true);
            Scrum plain = scrum(11L, title, false);
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(TITLE_IDS, USER_ID))
                    .willReturn(List.of(title));
            given(scrumQueryPort.findAllByTitleIdAndUserId(TITLE_ID, USER_ID))
                    .willReturn(List.of(starred, plain));

            // when
            service.deleteScrumTitle(new DeleteScrumTitleCommand(USER_ID, TITLE_ID));

            // then
            Set<Long> expectedScrumIds = Set.of(10L, 11L);
            verify(starImageCascadeCleaner).cleanupByScrumIds(expectedScrumIds);
            verify(scrumWritePort).softDeleteAllByIdIn(expectedScrumIds);
            verify(starRecordCascadePort).cascadeDeleteByScrumIdIn(expectedScrumIds);
            verify(scrumTitleRepositoryPort).softDeleteAllByIds(List.of(TITLE_ID));
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("타 유저의 titleId면 TITLE_NOT_FOUND를 던지고 어떤 쓰기도 하지 않는다")
        void should_throwTitleNotFound_when_titleNotOwnedByUser() {
            // given
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(TITLE_IDS, USER_ID))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(
                            () ->
                                    service.deleteScrumTitle(
                                            new DeleteScrumTitleCommand(USER_ID, TITLE_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TITLE_NOT_FOUND);
            verifyNoWrites();
        }

        @Test
        @DisplayName("미존재 titleId면 TITLE_NOT_FOUND를 던지고 어떤 쓰기도 하지 않는다")
        void should_throwTitleNotFound_when_titleDoesNotExist() {
            // given
            given(scrumTitleRepositoryPort.findAllByIdInAndUserId(TITLE_IDS, USER_ID))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(
                            () ->
                                    service.deleteScrumTitle(
                                            new DeleteScrumTitleCommand(USER_ID, TITLE_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TITLE_NOT_FOUND);
            verifyNoWrites();
        }

        private void verifyNoWrites() {
            verify(starImageCascadeCleaner, never()).cleanupByScrumIds(anyCollection());
            verify(scrumWritePort, never()).softDeleteAllByIdIn(anyCollection());
            verify(starRecordCascadePort, never()).cascadeDeleteByScrumIdIn(anyCollection());
            verify(scrumTitleRepositoryPort, never()).softDeleteAllByIds(anyList());
            verify(scrumQueryPort, never()).findAllByTitleIdAndUserId(anyLong(), anyLong());
        }
    }

    // ============== helpers ==============

    private static ScrumTitle title(Long id) {
        Project project = Project.builder().id(1000L + id).name("P").build();
        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", id);
        ReflectionTestUtils.setField(title, "project", project);
        ReflectionTestUtils.setField(title, "freeText", "F");
        return title;
    }

    private static Scrum scrum(Long id, ScrumTitle title, boolean hasStar) {
        User user = User.createForSocialLogin();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        ReflectionTestUtils.setField(title, "user", user);
        Scrum scrum = Scrum.create(user, title, "content", LocalDate.of(2026, 5, 23));
        ReflectionTestUtils.setField(scrum, "id", id);
        ReflectionTestUtils.setField(scrum, "hasStar", hasStar);
        return scrum;
    }
}
