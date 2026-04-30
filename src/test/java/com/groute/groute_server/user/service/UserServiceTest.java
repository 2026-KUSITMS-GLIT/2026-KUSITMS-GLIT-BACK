package com.groute.groute_server.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.enums.JobRole;
import com.groute.groute_server.user.enums.UserStatus;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long USER_ID = 1L;

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    @Nested
    @DisplayName("내 프로필 조회")
    class GetMyProfile {

        @Test
        @DisplayName("성공 — 유저 존재 시 엔티티 반환")
        void returnsUser_whenUserExists() {
            User user = User.createForSocialLogin();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            User result = userService.getMyProfile(USER_ID);

            assertThat(result).isSameAs(user);
        }

        @Test
        @DisplayName("실패 — 유저 없으면 USER_NOT_FOUND")
        void throwsUserNotFound_whenUserMissing() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMyProfile(USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("내 프로필 수정")
    class UpdateMyProfile {

        @Test
        @DisplayName("성공 — 라벨을 enum으로 변환해 엔티티에 반영")
        void updatesEntity_whenLabelsValid() {
            User user = User.createForSocialLogin();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            User result = userService.updateMyProfile(USER_ID, "개발자", "재학 중");

            assertThat(result).isSameAs(user);
            assertThat(result.getJobRole()).isEqualTo(JobRole.DEVELOPER);
            assertThat(result.getUserStatus()).isEqualTo(UserStatus.STUDENT);
        }

        @Test
        @DisplayName("실패 — 직군 라벨 이상 시 INVALID_JOB_ROLE, 유저 조회 스킵")
        void throwsInvalidJobRole_whenJobRoleUnknown() {
            assertThatThrownBy(() -> userService.updateMyProfile(USER_ID, "몰라요", "재학 중"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.INVALID_JOB_ROLE);

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("실패 — 상태 라벨 이상 시 INVALID_USER_STATUS, 유저 조회 스킵")
        void throwsInvalidUserStatus_whenStatusUnknown() {
            assertThatThrownBy(() -> userService.updateMyProfile(USER_ID, "개발자", "백수"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.INVALID_USER_STATUS);

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("실패 — 유효 라벨이지만 유저 없으면 USER_NOT_FOUND")
        void throwsUserNotFound_whenUserMissing() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateMyProfile(USER_ID, "개발자", "재학 중"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }
}
