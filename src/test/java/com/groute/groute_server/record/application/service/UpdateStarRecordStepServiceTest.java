package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.in.star.UpdateStarRecordStepCommand;
import com.groute.groute_server.record.application.port.out.ProjectPort;
import com.groute.groute_server.record.application.port.out.UserPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.application.port.out.user.UserStreakPort;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.ScrumTitleStatus;
import com.groute.groute_server.record.domain.enums.StarStep;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class UpdateStarRecordStepServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ANOTHER_USER_ID = 2L;
    private static final Long STAR_ID = 100L;
    private static final LocalDate DATE = LocalDate.of(2026, 5, 9);

    @Mock StarRecordRepositoryPort starRecordRepositoryPort;
    @Mock ScrumWritePort scrumWritePort;
    @Mock ScrumQueryPort scrumQueryPort;
    @Mock ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    @Mock ProjectPort projectPort;
    @Mock UserStreakPort userStreakPort;
    @Mock UserPort userPort;

    @InjectMocks UpdateStarRecordStepService service;

    private User owner;
    private User mockOwner;
    private ScrumTitle title;
    private Scrum scrum;
    private StarRecord record;

    @BeforeEach
    void setUp() {
        owner = User.createForSocialLogin();
        ReflectionTestUtils.setField(owner, "id", USER_ID);

        mockOwner = Mockito.mock(User.class);
        Mockito.lenient().when(mockOwner.getId()).thenReturn(USER_ID);

        Project project = Project.builder().name("프로젝트").build();
        ReflectionTestUtils.setField(project, "id", 100L);

        title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", 10L);
        ReflectionTestUtils.setField(title, "user", owner);
        ReflectionTestUtils.setField(title, "status", ScrumTitleStatus.PENDING);
        ReflectionTestUtils.setField(title, "project", project);

        scrum = Scrum.create(owner, title, "내용", DATE);
        ReflectionTestUtils.setField(scrum, "id", 50L);

        record = StarRecord.create(owner, scrum);
        ReflectionTestUtils.setField(record, "id", STAR_ID);
    }

    @Nested
    @DisplayName("예외 케이스")
    class Validation {

        @Test
        @DisplayName("존재하지 않는 starRecordId면 STAR_NOT_FOUND를 던진다")
        void should_throwStarNotFound_when_notExist() {
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStep(command(StarStep.ST)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_NOT_FOUND);
        }

        @Test
        @DisplayName("본인 소유가 아닌 StarRecord면 STAR_FORBIDDEN을 던진다")
        void should_throwForbidden_when_notOwner() {
            User anotherUser = User.createForSocialLogin();
            ReflectionTestUtils.setField(anotherUser, "id", ANOTHER_USER_ID);
            ScrumTitle anotherTitle = new ScrumTitle();
            ReflectionTestUtils.setField(anotherTitle, "user", anotherUser);
            Scrum anotherScrum = Scrum.create(anotherUser, anotherTitle, "내용", DATE);
            StarRecord otherRecord = StarRecord.create(anotherUser, anotherScrum);
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(otherRecord));

            assertThatThrownBy(() -> service.updateStep(command(StarStep.ST)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_FORBIDDEN);
        }

        @Test
        @DisplayName("이미 완료된(WRITTEN) StarRecord면 STAR_WRITE_LOCKED를 던진다")
        void should_throwWriteLocked_when_alreadyWritten() {
            record.saveStep(StarStep.ST, "ST 답변");
            record.saveStep(StarStep.A, "A 답변");
            record.complete(OffsetDateTime.now());
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(record));

            assertThatThrownBy(() -> service.updateStep(command(StarStep.ST)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STAR_WRITE_LOCKED);
        }
    }

    @Nested
    @DisplayName("단계 저장 — 중간 단계")
    class IntermediateStep {

        @Test
        @DisplayName("ST 단계 저장 시 completeStar는 호출되지 않는다")
        void should_notComplete_when_stepIsST() {
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(record));

            service.updateStep(command(StarStep.ST));

            verify(scrumWritePort, never()).completeStar(anyLong());
        }

        @Test
        @DisplayName("A 단계 저장 시 completeStar는 호출되지 않는다")
        void should_notComplete_when_stepIsA() {
            record.saveStep(StarStep.ST, "ST 답변");
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(record));

            service.updateStep(command(StarStep.A));

            verify(scrumWritePort, never()).completeStar(anyLong());
        }
    }

    @Nested
    @DisplayName("R 단계 완료 처리")
    class CompleteStep {

        @BeforeEach
        void setUpR() {
            record.saveStep(StarStep.ST, "ST 답변");
            record.saveStep(StarStep.A, "A 답변");
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(record));
            Mockito.lenient()
                    .when(scrumQueryPort.findAllByUserAndDate(any(), any()))
                    .thenReturn(List.of(scrum));
            Mockito.lenient().when(userPort.findById(USER_ID)).thenReturn(mockOwner);
        }

        @Test
        @DisplayName("R 단계 저장 시 record.complete()와 scrumWritePort.completeStar가 호출된다")
        void should_callCompleteStar_when_stepIsR() {
            given(scrumWritePort.completeStar(scrum.getId())).willReturn(1);
            given(starRecordRepositoryPort.countCompleted(USER_ID)).willReturn(1L);

            service.updateStep(command(StarStep.R));

            verify(scrumWritePort).completeStar(scrum.getId());
        }

        @Test
        @DisplayName("completeStar가 0을 반환하면 SCRUM_NOT_FOUND를 던진다")
        void should_throwScrumNotFound_when_completeStarUpdatesNothing() {
            given(scrumWritePort.completeStar(scrum.getId())).willReturn(0);

            assertThatThrownBy(() -> service.updateStep(command(StarStep.R)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_NOT_FOUND);
        }

        @Test
        @DisplayName("R 단계 완료 시 isCompleted=true로 세팅된다")
        void should_setIsCompleted_when_stepIsR() {
            given(scrumWritePort.completeStar(scrum.getId())).willReturn(1);
            given(starRecordRepositoryPort.countCompleted(USER_ID)).willReturn(1L);

            service.updateStep(command(StarStep.R));

            org.assertj.core.api.Assertions.assertThat(record.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("R 단계 완료 시 commitAllByIds가 호출된다")
        void should_callCommitAllByIds_when_stepIsR() {
            given(scrumWritePort.completeStar(scrum.getId())).willReturn(1);
            given(starRecordRepositoryPort.countCompleted(USER_ID)).willReturn(1L);

            service.updateStep(command(StarStep.R));

            verify(scrumTitleRepositoryPort).commitAllByIds(any());
        }

        @Test
        @DisplayName("완료 개수 1회일 때 코치마크 노출 플래그가 세팅된다")
        void should_markCoachMark_when_completedCountIs1() {
            given(scrumWritePort.completeStar(scrum.getId())).willReturn(1);
            given(starRecordRepositoryPort.countCompleted(USER_ID)).willReturn(1L);

            service.updateStep(command(StarStep.R));

            verify(mockOwner).markPendingCoachMark();
        }

        @Test
        @DisplayName("완료 개수 10회일 때 MINI 리포트 모달 노출 플래그가 세팅된다")
        void should_markMiniReportModal_when_completedCountIs10() {
            given(scrumWritePort.completeStar(scrum.getId())).willReturn(1);
            given(starRecordRepositoryPort.countCompleted(USER_ID)).willReturn(10L);

            service.updateStep(command(StarStep.R));

            verify(mockOwner).markPendingReportModal("MINI");
        }

        @Test
        @DisplayName("완료 개수 20회일 때 FULL 리포트 모달 노출 플래그가 세팅된다")
        void should_markFullReportModal_when_completedCountIs20() {
            given(scrumWritePort.completeStar(scrum.getId())).willReturn(1);
            given(starRecordRepositoryPort.countCompleted(USER_ID)).willReturn(20L);

            service.updateStep(command(StarStep.R));

            verify(mockOwner).markPendingReportModal("FULL");
        }
    }

    @Nested
    @DisplayName("확정 시점 카운터 반영 (QA-34)")
    class CommitCounters {

        @BeforeEach
        void setUpR() {
            record.saveStep(StarStep.ST, "ST 답변");
            record.saveStep(StarStep.A, "A 답변");
            given(starRecordRepositoryPort.findByIdWithScrum(STAR_ID))
                    .willReturn(Optional.of(record));
            given(scrumWritePort.completeStar(scrum.getId())).willReturn(1);
            given(starRecordRepositoryPort.countCompleted(USER_ID)).willReturn(1L);
            Mockito.lenient()
                    .when(scrumQueryPort.findAllByUserAndDate(any(), any()))
                    .thenReturn(List.of(scrum));
            Mockito.lenient().when(userPort.findById(USER_ID)).thenReturn(mockOwner);
        }

        @Test
        @DisplayName("PENDING title만 COMMITTED로 전환하고 COMMITTED title은 제외한다")
        void should_commitOnlyPendingTitles_when_stepIsR() {
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE))
                    .willReturn(
                            List.of(scrum, scrumWith(51L, 11L, ScrumTitleStatus.COMMITTED, 101L)));

            service.updateStep(command(StarStep.R));

            verify(scrumTitleRepositoryPort).commitAllByIds(Set.of(10L));
        }

        @Test
        @DisplayName("전환된 title 수만큼 project별 titleCount를 증가시킨다")
        void should_incrementTitleCountPerProject_when_stepIsR() {
            given(scrumQueryPort.findAllByUserAndDate(USER_ID, DATE))
                    .willReturn(
                            List.of(
                                    scrum,
                                    scrumWith(51L, 11L, ScrumTitleStatus.PENDING, 100L),
                                    scrumWith(52L, 12L, ScrumTitleStatus.PENDING, 101L)));

            service.updateStep(command(StarStep.R));

            verify(projectPort).applyTitleCountIncrement(100L, 2);
            verify(projectPort).applyTitleCountIncrement(101L, 1);
        }

        @Test
        @DisplayName("user streak를 scrum 일자로 갱신한다")
        void should_recordStreak_when_stepIsR() {
            service.updateStep(command(StarStep.R));

            verify(userStreakPort).recordOnDate(USER_ID, DATE);
        }

        @Test
        @DisplayName("같은 일자 두 번째 STAR 완료 — 이미 COMMITTED면 commit·titleCount 증가를 수행하지 않는다")
        void should_notIncrementTitleCount_when_allTitlesAlreadyCommitted() {
            title.commit();

            service.updateStep(command(StarStep.R));

            verify(scrumTitleRepositoryPort, never()).commitAllByIds(any());
            verify(projectPort, never()).applyTitleCountIncrement(anyLong(), anyInt());
            verify(userStreakPort).recordOnDate(USER_ID, DATE);
        }
    }

    // ============== helpers ==============

    private UpdateStarRecordStepCommand command(StarStep step) {
        return new UpdateStarRecordStepCommand(USER_ID, STAR_ID, step, "답변 내용");
    }

    /** 지정 status·project를 가진 ScrumTitle 산하의 Scrum을 만든다. 소유자는 {@link #owner} 고정. */
    private Scrum scrumWith(Long scrumId, Long titleId, ScrumTitleStatus status, Long projectId) {
        Project project = Project.builder().name("P" + projectId).build();
        ReflectionTestUtils.setField(project, "id", projectId);

        ScrumTitle title = new ScrumTitle();
        ReflectionTestUtils.setField(title, "id", titleId);
        ReflectionTestUtils.setField(title, "user", owner);
        ReflectionTestUtils.setField(title, "status", status);
        ReflectionTestUtils.setField(title, "project", project);

        Scrum created = Scrum.create(owner, title, "내용", DATE);
        ReflectionTestUtils.setField(created, "id", scrumId);
        return created;
    }
}
