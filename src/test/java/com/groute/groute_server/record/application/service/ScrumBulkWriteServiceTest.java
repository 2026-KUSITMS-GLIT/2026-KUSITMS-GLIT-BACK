package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import com.groute.groute_server.record.application.port.in.scrum.BulkWriteScrumCommand;
import com.groute.groute_server.record.application.port.in.scrum.BulkWriteScrumResult;
import com.groute.groute_server.record.application.port.out.ProjectPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumQueryPort;
import com.groute.groute_server.record.application.port.out.scrum.ScrumWritePort;
import com.groute.groute_server.record.application.port.out.scrumtitle.ScrumTitleRepositoryPort;
import com.groute.groute_server.record.application.port.out.user.UserReferencePort;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.ScrumTitle;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class ScrumBulkWriteServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 5, 9);

    @Mock ProjectPort projectPort;
    @Mock ScrumQueryPort scrumQueryPort;
    @Mock ScrumTitleRepositoryPort scrumTitleRepositoryPort;
    @Mock ScrumWritePort scrumWritePort;
    @Mock UserReferencePort userReferencePort;

    @InjectMocks ScrumBulkWriteService service;

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("같은 날짜에 이미 스크럼이 존재하면 SCRUM_DATE_ALREADY_WRITTEN을 던진다")
        void should_throwDateAlreadyWritten_when_scrumExistsOnDate() {
            given(scrumQueryPort.existsByUserAndDate(USER_ID, DATE)).willReturn(true);

            assertThatThrownBy(() -> service.bulkWrite(command(group(100L, "제목", "스크럼1"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_DATE_ALREADY_WRITTEN);

            verify(scrumTitleRepositoryPort, never()).saveAll(anyList());
            verify(scrumWritePort, never()).saveAll(anyCollection());
        }

        @Test
        @DisplayName("스크럼 합계가 6개면 SCRUM_DATE_LIMIT_EXCEEDED를 던진다")
        void should_throwDateLimitExceeded_when_6Scrums() {
            BulkWriteScrumCommand command =
                    command(group(100L, "제목", "a", "b", "c", "d", "e", "f"));

            assertThatThrownBy(() -> service.bulkWrite(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_DATE_LIMIT_EXCEEDED);

            verify(scrumTitleRepositoryPort, never()).saveAll(anyList());
            verify(scrumWritePort, never()).saveAll(anyCollection());
        }

        @Test
        @DisplayName("여러 그룹에 걸쳐 합산 6개면 SCRUM_DATE_LIMIT_EXCEEDED를 던진다")
        void should_throwDateLimitExceeded_when_totalAcrossGroupsExceeds5() {
            BulkWriteScrumCommand command =
                    command(group(100L, "제목1", "a", "b", "c"), group(101L, "제목2", "d", "e", "f"));

            assertThatThrownBy(() -> service.bulkWrite(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCRUM_DATE_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("projectId가 본인 소유가 아니면 PROJECT_NOT_FOUND를 던진다")
        void should_throwProjectNotFound_when_projectNotOwned() {
            given(projectPort.findByIdAndUserId(anyLong(), anyLong())).willReturn(Optional.empty());
            BulkWriteScrumCommand command = command(group(100L, "제목", "스크럼1"));

            assertThatThrownBy(() -> service.bulkWrite(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);

            verify(scrumTitleRepositoryPort, never()).saveAll(anyList());
            verify(scrumWritePort, never()).saveAll(anyCollection());
        }
    }

    @Nested
    @DisplayName("저장 성공")
    class Save {

        @Test
        @DisplayName("단일 그룹 단일 스크럼을 저장하고 결과를 올바르게 반환한다")
        void should_returnCorrectResult_when_singleGroupSingleScrum() {
            Project project = project(100L, "프로젝트A");
            given(projectPort.findByIdAndUserId(100L, USER_ID)).willReturn(Optional.of(project));
            given(userReferencePort.getReferenceById(USER_ID))
                    .willReturn(User.createForSocialLogin());
            stubSaveTitles();
            stubSaveScrums();

            BulkWriteScrumResult result = service.bulkWrite(command(group(100L, "내 제목", "스크럼1")));

            assertThat(result.groups()).hasSize(1);
            BulkWriteScrumResult.GroupResult group = result.groups().get(0);
            assertThat(group.projectName()).isEqualTo("프로젝트A");
            assertThat(group.freeText()).isEqualTo("내 제목");
            assertThat(group.scrums()).hasSize(1);
            assertThat(group.scrums().get(0).content()).isEqualTo("스크럼1");
            assertThat(group.scrums().get(0).scrumId()).isNotNull();
        }

        @Test
        @DisplayName("정확히 5개의 스크럼은 예외 없이 저장된다")
        void should_saveScrums_when_exactly5Scrums() {
            given(projectPort.findByIdAndUserId(anyLong(), anyLong()))
                    .willReturn(Optional.of(project(100L, "프로젝트A")));
            given(userReferencePort.getReferenceById(USER_ID))
                    .willReturn(User.createForSocialLogin());
            stubSaveTitles();
            stubSaveScrums();

            BulkWriteScrumResult result =
                    service.bulkWrite(command(group(100L, "제목", "a", "b", "c", "d", "e")));

            assertThat(result.groups().get(0).scrums()).hasSize(5);
        }

        @Test
        @DisplayName("여러 그룹 저장 시 각 프로젝트에 titleCount +1을 수행한다")
        void should_incrementTitleCount_when_multipleGroups() {
            given(projectPort.findByIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(project(100L, "프로젝트A")));
            given(projectPort.findByIdAndUserId(101L, USER_ID))
                    .willReturn(Optional.of(project(101L, "프로젝트B")));
            given(userReferencePort.getReferenceById(USER_ID))
                    .willReturn(User.createForSocialLogin());
            stubSaveTitles();
            stubSaveScrums();

            service.bulkWrite(command(group(100L, "제목1", "A", "B"), group(101L, "제목2", "C")));

            verify(projectPort).applyTitleCountIncrement(100L, 1);
            verify(projectPort).applyTitleCountIncrement(101L, 1);
        }

        @Test
        @DisplayName("동일 projectId가 두 그룹에 쓰이면 titleCount를 2 증가한다")
        void should_incrementTitleCountByTwo_when_sameProjectInTwoGroups() {
            given(projectPort.findByIdAndUserId(anyLong(), anyLong()))
                    .willReturn(Optional.of(project(100L, "프로젝트A")));
            given(userReferencePort.getReferenceById(USER_ID))
                    .willReturn(User.createForSocialLogin());
            stubSaveTitles();
            stubSaveScrums();

            service.bulkWrite(command(group(100L, "제목1", "A"), group(100L, "제목2", "B")));

            verify(projectPort).applyTitleCountIncrement(100L, 2);
        }

        @Test
        @DisplayName("여러 그룹의 스크럼 순서가 결과에 올바르게 매핑된다")
        void should_mapScrumResultsInOrder_when_multipleGroups() {
            given(projectPort.findByIdAndUserId(100L, USER_ID))
                    .willReturn(Optional.of(project(100L, "P1")));
            given(projectPort.findByIdAndUserId(101L, USER_ID))
                    .willReturn(Optional.of(project(101L, "P2")));
            given(userReferencePort.getReferenceById(USER_ID))
                    .willReturn(User.createForSocialLogin());
            stubSaveTitles();
            stubSaveScrums();

            BulkWriteScrumResult result =
                    service.bulkWrite(
                            command(group(100L, "제목1", "A", "B"), group(101L, "제목2", "C")));

            assertThat(result.groups()).hasSize(2);
            assertThat(result.groups().get(0).scrums()).hasSize(2);
            assertThat(result.groups().get(0).scrums().get(0).content()).isEqualTo("A");
            assertThat(result.groups().get(0).scrums().get(1).content()).isEqualTo("B");
            assertThat(result.groups().get(1).scrums()).hasSize(1);
            assertThat(result.groups().get(1).scrums().get(0).content()).isEqualTo("C");
        }
    }

    // ============== helpers ==============

    private static BulkWriteScrumCommand command(BulkWriteScrumCommand.GroupCommand... groups) {
        return new BulkWriteScrumCommand(USER_ID, DATE, List.of(groups));
    }

    private static BulkWriteScrumCommand.GroupCommand group(
            Long projectId, String freeText, String... contents) {
        return new BulkWriteScrumCommand.GroupCommand(projectId, freeText, List.of(contents));
    }

    private static Project project(Long id, String name) {
        Project p = Project.builder().name(name).build();
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private void stubSaveTitles() {
        given(scrumTitleRepositoryPort.saveAll(anyList()))
                .willAnswer(
                        inv -> {
                            List<ScrumTitle> titles = new ArrayList<>(inv.getArgument(0));
                            for (int i = 0; i < titles.size(); i++) {
                                ReflectionTestUtils.setField(titles.get(i), "id", (long) (i + 1));
                            }
                            return titles;
                        });
    }

    private void stubSaveScrums() {
        given(scrumWritePort.saveAll(anyCollection()))
                .willAnswer(
                        inv -> {
                            List<Scrum> scrums = new ArrayList<>(inv.getArgument(0));
                            for (int i = 0; i < scrums.size(); i++) {
                                ReflectionTestUtils.setField(scrums.get(i), "id", (long) (i + 10));
                            }
                            return scrums;
                        });
    }
}
