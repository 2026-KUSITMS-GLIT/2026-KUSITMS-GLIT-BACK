package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.common.transaction.AfterCommitExecutor;
import com.groute.groute_server.record.application.port.in.scrum.DeleteScrumCommand;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordCascadePort;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class DeleteScrumServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SCRUM_ID = 10L;
    private static final Long TITLE_ID = 100L;
    private static final Set<Long> SCRUM_IDS = Set.of(SCRUM_ID);

    @Mock ScrumQueryPort scrumQueryPort;
    @Mock ScrumWritePort scrumWritePort;
    @Mock ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    @Mock StarRecordCascadePort starRecordCascadePort;
    @Mock StarImageCascadeCleaner starImageCascadeCleaner;
    @Mock CacheManager cacheManager;
    @Mock AfterCommitExecutor afterCommitExecutor;

    @InjectMocks DeleteScrumService service;

    @Nested
    @DisplayName("정상 삭제")
    class HappyPath {

        @Test
        @DisplayName("hasStar=true 스크럼은 이미지 cleanup + soft-delete + STAR cascade + 카운터 -1을 모두 호출한다")
        void should_softDeleteScrum_andCascadeStar_when_scrumHasStar() {
            // given
            ScrumTitle title = title(TITLE_ID);
            Scrum scrum = scrum(SCRUM_ID, title, true);
            given(scrumQueryPort.findAllByIdInAndUserId(SCRUM_IDS, USER_ID))
                    .willReturn(List.of(scrum));

            // when
            assertThatCode(() -> service.deleteScrum(new DeleteScrumCommand(USER_ID, SCRUM_ID)))
                    .doesNotThrowAnyException();

            // then
            verify(starImageCascadeCleaner).cleanupByScrumIds(SCRUM_IDS);
            verify(scrumWritePort).softDeleteAllByIdIn(SCRUM_IDS);
            verify(starRecordCascadePort).cascadeDeleteByScrumIdIn(SCRUM_IDS);
            verify(scrumTitleRepositoryPort).applyScrumCountIncrement(TITLE_ID, -1);
        }

        @Test
        @DisplayName("hasStar=false 스크럼도 동일하게 이미지 cleanup·cascade를 호출한다(빈 집합이라도 일관 호출)")
        void should_softDeleteScrum_when_scrumWithoutStar() {
            // given
            ScrumTitle title = title(TITLE_ID);
            Scrum scrum = scrum(SCRUM_ID, title, false);
            given(scrumQueryPort.findAllByIdInAndUserId(SCRUM_IDS, USER_ID))
                    .willReturn(List.of(scrum));

            // when
            service.deleteScrum(new DeleteScrumCommand(USER_ID, SCRUM_ID));

            // then
            verify(starImageCascadeCleaner).cleanupByScrumIds(SCRUM_IDS);
            verify(scrumWritePort).softDeleteAllByIdIn(SCRUM_IDS);
            verify(starRecordCascadePort).cascadeDeleteByScrumIdIn(SCRUM_IDS);
            verify(scrumTitleRepositoryPort).applyScrumCountIncrement(TITLE_ID, -1);
        }

        @Test
        @DisplayName("ScrumTitle.scrumCount는 정확히 1 감소(-1)만 호출된다")
        void should_decrementScrumCount_byOne() {
            // given
            ScrumTitle title = title(TITLE_ID);
            Scrum scrum = scrum(SCRUM_ID, title, false);
            given(scrumQueryPort.findAllByIdInAndUserId(SCRUM_IDS, USER_ID))
                    .willReturn(List.of(scrum));

            // when
            service.deleteScrum(new DeleteScrumCommand(USER_ID, SCRUM_ID));

            // then
            verify(scrumTitleRepositoryPort).applyScrumCountIncrement(TITLE_ID, -1);
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("타 유저의 스크럼이면 SCRUM_NOT_FOUND를 던지고 어떤 쓰기도 하지 않는다")
        void should_throwScrumNotFound_when_scrumNotOwnedByUser() {
            // given — 본인 소유가 아니므로 쿼리 결과가 비어 있음
            given(scrumQueryPort.findAllByIdInAndUserId(SCRUM_IDS, USER_ID)).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> service.deleteScrum(new DeleteScrumCommand(USER_ID, SCRUM_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_NOT_FOUND);
            verifyNoWrites();
        }

        @Test
        @DisplayName("미존재 scrumId면 SCRUM_NOT_FOUND를 던지고 어떤 쓰기도 하지 않는다")
        void should_throwScrumNotFound_when_scrumDoesNotExist() {
            // given
            given(scrumQueryPort.findAllByIdInAndUserId(SCRUM_IDS, USER_ID)).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> service.deleteScrum(new DeleteScrumCommand(USER_ID, SCRUM_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_NOT_FOUND);
            verifyNoWrites();
        }

        private void verifyNoWrites() {
            verify(starImageCascadeCleaner, never()).cleanupByScrumIds(anyCollection());
            verify(scrumWritePort, never()).softDeleteAllByIdIn(anyCollection());
            verify(starRecordCascadePort, never()).cascadeDeleteByScrumIdIn(anyCollection());
            verify(scrumTitleRepositoryPort, never()).applyScrumCountIncrement(anyLong(), anyInt());
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
        Scrum scrum = Scrum.create(user, title, "content", java.time.LocalDate.of(2026, 5, 4));
        ReflectionTestUtils.setField(scrum, "id", id);
        ReflectionTestUtils.setField(scrum, "hasStar", hasStar);
        return scrum;
    }
}
