package com.groute.groute_server.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.config.UserProperties;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.enums.JobRole;
import com.groute.groute_server.user.enums.UserStatus;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long USER_ID = 1L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T12:00:00Z");
    private static final int GRACE_DAYS = 30;

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserProperties userProperties;
    @Mock private Clock clock;

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
    @DisplayName("온보딩 완료")
    class CompleteOnboarding {

        @Test
        @DisplayName("성공 — 조건부 업데이트 성공 시 findById 결과 반환")
        void completesOnboarding_whenValid() {
            User user = User.createForSocialLogin();
            given(
                            userRepository.completeOnboardingIfNotDone(
                                    USER_ID, "겨레", JobRole.DEVELOPER, UserStatus.STUDENT))
                    .willReturn(1);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            User result = userService.completeOnboarding(USER_ID, "겨레", "개발자", "재학 중");

            assertThat(result).isSameAs(user);
        }

        @Test
        @DisplayName("실패 — 이미 온보딩 완료된 유저면 ONBOARDING_ALREADY_COMPLETED")
        void throwsAlreadyCompleted_whenNicknameExists() {
            given(
                            userRepository.completeOnboardingIfNotDone(
                                    USER_ID, "새닉네임", JobRole.DEVELOPER, UserStatus.STUDENT))
                    .willReturn(0);
            given(userRepository.existsById(USER_ID)).willReturn(true);

            assertThatThrownBy(() -> userService.completeOnboarding(USER_ID, "새닉네임", "개발자", "재학 중"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }

        @Test
        @DisplayName("실패 — 유저 없으면 USER_NOT_FOUND")
        void throwsUserNotFound_whenUserMissing() {
            given(
                            userRepository.completeOnboardingIfNotDone(
                                    USER_ID, "겨레", JobRole.DEVELOPER, UserStatus.STUDENT))
                    .willReturn(0);
            given(userRepository.existsById(USER_ID)).willReturn(false);

            assertThatThrownBy(() -> userService.completeOnboarding(USER_ID, "겨레", "개발자", "재학 중"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 — 유효하지 않은 직군 라벨이면 INVALID_JOB_ROLE")
        void throwsInvalidJobRole_whenJobRoleUnknown() {
            assertThatThrownBy(() -> userService.completeOnboarding(USER_ID, "겨레", "몰라요", "재학 중"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.INVALID_JOB_ROLE);

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("실패 — 유효하지 않은 상태 라벨이면 INVALID_USER_STATUS")
        void throwsInvalidUserStatus_whenStatusUnknown() {
            assertThatThrownBy(() -> userService.completeOnboarding(USER_ID, "겨레", "개발자", "백수"))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.INVALID_USER_STATUS);

            verifyNoInteractions(userRepository);
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

    @Nested
    @DisplayName("회원 탈퇴")
    class DeleteMyAccount {

        @BeforeEach
        void setUpClockAndProperties() {
            // USER_NOT_FOUND 케이스는 clock·properties 도달 전 short-circuit이므로 lenient 처리.
            lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
            lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
            lenient().when(userProperties.hardDeleteGraceDays()).thenReturn(GRACE_DAYS);
        }

        @Test
        @DisplayName("성공 — 활성 유저 soft delete + 30일 뒤 hardDeleteAt 예약")
        void softDeletesAndSchedulesHardDelete_whenUserActive() {
            User user = User.createForSocialLogin();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            userService.deleteMyAccount(USER_ID);

            OffsetDateTime expected =
                    OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).plusDays(GRACE_DAYS);
            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getHardDeleteAt()).isEqualTo(expected);
        }

        @Test
        @DisplayName("성공 — 활성 유저 탈퇴 시 refresh token 즉시 무효화")
        void invalidatesRefreshToken_whenUserActive() {
            User user = User.createForSocialLogin();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            userService.deleteMyAccount(USER_ID);

            verify(refreshTokenRepository).deleteByUserId(USER_ID);
        }

        @Test
        @DisplayName("성공 — 이미 탈퇴된 유저 재호출 시 hardDeleteAt 미변경 (멱등) + refresh token은 그대로 호출")
        void remainsIdempotent_whenUserAlreadyDeleted() {
            User user = User.createForSocialLogin();
            // 과거 다른 시각으로 이미 탈퇴 처리된 상태를 만들어둔다.
            Clock earlier = Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC);
            user.scheduleHardDelete(earlier, Duration.ofDays(GRACE_DAYS));
            OffsetDateTime originalHardDeleteAt = user.getHardDeleteAt();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            userService.deleteMyAccount(USER_ID);

            assertThat(user.getHardDeleteAt()).isEqualTo(originalHardDeleteAt);
            verify(refreshTokenRepository).deleteByUserId(USER_ID);
        }

        @Test
        @DisplayName("실패 — 유저 없으면 USER_NOT_FOUND, refresh token 미호출")
        void throwsUserNotFound_whenUserMissing() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteMyAccount(USER_ID))
                    .asInstanceOf(InstanceOfAssertFactories.type(BusinessException.class))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verifyNoInteractions(refreshTokenRepository);
        }
    }
}
