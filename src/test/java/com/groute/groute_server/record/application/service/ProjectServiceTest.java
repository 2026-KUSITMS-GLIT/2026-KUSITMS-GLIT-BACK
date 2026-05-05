package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.application.port.out.ProjectPort;
import com.groute.groute_server.record.application.port.out.UserPort;
import com.groute.groute_server.record.domain.Project;
import com.groute.groute_server.record.domain.ProjectPage;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final long USER_ID = 1L;
    private static final long PROJECT_ID = 10L;

    @Mock private ProjectPort projectPort;
    @Mock private UserPort userPort;

    @InjectMocks private ProjectService projectService;

    private Project buildProject(String name, short titleCount) {
        User user = User.createForSocialLogin();
        Project project = Project.builder().user(user).name(name).build();
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        ReflectionTestUtils.setField(project, "titleCount", titleCount);
        return project;
    }

    @Nested
    @DisplayName("프로젝트 태그 생성")
    class CreateProject {

        @Test
        @DisplayName("성공 — 저장된 프로젝트 반환")
        void createsProject_whenValid() {
            User user = User.createForSocialLogin();
            Project project = buildProject("졸업프로젝트", (short) 0);
            given(projectPort.existsByUserIdAndName(USER_ID, "졸업프로젝트")).willReturn(false);
            given(userPort.findById(USER_ID)).willReturn(user);
            given(projectPort.save(org.mockito.ArgumentMatchers.any())).willReturn(project);

            Project result = projectService.createProject(USER_ID, "졸업프로젝트");

            assertThat(result).isSameAs(project);
        }

        @Test
        @DisplayName("실패 — 이름 중복이면 PROJECT_NAME_DUPLICATE")
        void throwsDuplicate_whenNameExists() {
            given(projectPort.existsByUserIdAndName(USER_ID, "졸업프로젝트")).willReturn(true);

            assertThatThrownBy(() -> projectService.createProject(USER_ID, "졸업프로젝트"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.PROJECT_NAME_DUPLICATE);

            verifyNoInteractions(userPort);
        }

        @Test
        @DisplayName("실패 — 유저 없으면 USER_NOT_FOUND")
        void throwsUserNotFound_whenUserMissing() {
            given(projectPort.existsByUserIdAndName(USER_ID, "졸업프로젝트")).willReturn(false);
            given(userPort.findById(USER_ID))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> projectService.createProject(USER_ID, "졸업프로젝트"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("프로젝트 태그 목록 조회")
    class GetProjects {

        @Test
        @DisplayName("성공 — 페이지 결과 반환")
        void returnsPage_whenValid() {
            Project project = buildProject("졸업프로젝트", (short) 0);
            Page<Project> page = new PageImpl<>(List.of(project), PageRequest.of(0, 5), 1);
            given(projectPort.findAllByUserId(eq(USER_ID), any())).willReturn(page);

            ProjectPage result = projectService.getProjects(USER_ID, 0, 5);

            assertThat(result.content()).containsExactly(project);
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(5);
            assertThat(result.totalPages()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("프로젝트 태그 이름 수정")
    class UpdateProject {

        @Test
        @DisplayName("성공 — rename() 호출 후 프로젝트 반환")
        void updatesProject_whenValid() {
            Project project = buildProject("졸업프로젝트", (short) 0);
            given(projectPort.existsByUserIdAndName(USER_ID, "새이름")).willReturn(false);
            given(projectPort.findByIdAndUserId(PROJECT_ID, USER_ID))
                    .willReturn(Optional.of(project));

            Project result = projectService.updateProject(USER_ID, PROJECT_ID, "새이름");

            assertThat(result.getName()).isEqualTo("새이름");
        }

        @Test
        @DisplayName("실패 — 이름 중복이면 PROJECT_NAME_DUPLICATE")
        void throwsDuplicate_whenNameExists() {
            Project project = buildProject("졸업프로젝트", (short) 0);
            given(projectPort.findByIdAndUserId(PROJECT_ID, USER_ID))
                    .willReturn(Optional.of(project));
            given(projectPort.existsByUserIdAndName(USER_ID, "새이름")).willReturn(true);

            assertThatThrownBy(() -> projectService.updateProject(USER_ID, PROJECT_ID, "새이름"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.PROJECT_NAME_DUPLICATE);
        }

        @Test
        @DisplayName("실패 — 프로젝트 없으면 PROJECT_NOT_FOUND")
        void throwsNotFound_whenProjectMissing() {
            given(projectPort.findByIdAndUserId(PROJECT_ID, USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.updateProject(USER_ID, PROJECT_ID, "새이름"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("프로젝트 태그 삭제")
    class DeleteProject {

        @Test
        @DisplayName("성공 — softDelete() 호출")
        void deletesProject_whenNoneRecords() {
            Project project = buildProject("졸업프로젝트", (short) 0);
            given(projectPort.findByIdAndUserId(PROJECT_ID, USER_ID))
                    .willReturn(Optional.of(project));

            projectService.deleteProject(USER_ID, PROJECT_ID);

            assertThat(project.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("실패 — 프로젝트 없으면 PROJECT_NOT_FOUND")
        void throwsNotFound_whenProjectMissing() {
            given(projectPort.findByIdAndUserId(PROJECT_ID, USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.deleteProject(USER_ID, PROJECT_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 — 연결된 기록 있으면 PROJECT_HAS_RECORDS")
        void throwsHasRecords_whenTitleCountPositive() {
            Project project = buildProject("졸업프로젝트", (short) 1);
            given(projectPort.findByIdAndUserId(PROJECT_ID, USER_ID))
                    .willReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.deleteProject(USER_ID, PROJECT_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.PROJECT_HAS_RECORDS);
        }
    }
}
