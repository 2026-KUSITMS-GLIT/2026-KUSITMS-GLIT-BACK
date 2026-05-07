package com.groute.groute_server.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.auth.entity.DeviceToken;
import com.groute.groute_server.auth.enums.DevicePlatform;
import com.groute.groute_server.auth.repository.DeviceTokenRepository;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    private static final Long USER_ID = 1L;
    private static final String PUSH_TOKEN = "fcm-token-xyz";

    @Mock DeviceTokenRepository deviceTokenRepository;
    @Mock UserRepository userRepository;

    @InjectMocks DeviceTokenService deviceTokenService;

    @Nested
    @DisplayName("정상 등록")
    class HappyPath {

        @Test
        @DisplayName("푸시 토큰이 없으면 새 DeviceToken을 save한다")
        void should_saveNewToken_when_pushTokenNotExists() {
            // given
            User user = User.createForSocialLogin();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(deviceTokenRepository.findByPushToken(PUSH_TOKEN)).willReturn(Optional.empty());

            // when
            deviceTokenService.register(USER_ID, DevicePlatform.WEB, PUSH_TOKEN);

            // then
            ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
            verify(deviceTokenRepository).save(captor.capture());
            DeviceToken saved = captor.getValue();
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.WEB);
            assertThat(saved.getPushToken()).isEqualTo(PUSH_TOKEN);
            assertThat(saved.isActive()).isTrue();
        }

        @Test
        @DisplayName("푸시 토큰이 있으면 owner/platform/active만 갱신한다 (save 호출 X — dirty checking)")
        void should_updateExistingToken_when_pushTokenExists() {
            // given — 다른 유저(IOS, 비활성) 소유의 기존 토큰
            User newOwner = User.createForSocialLogin();
            User otherOwner = User.createForSocialLogin();
            DeviceToken existing = DeviceToken.register(otherOwner, DevicePlatform.IOS, PUSH_TOKEN);
            existing.deactivate();

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(newOwner));
            given(deviceTokenRepository.findByPushToken(PUSH_TOKEN))
                    .willReturn(Optional.of(existing));

            // when
            deviceTokenService.register(USER_ID, DevicePlatform.WEB, PUSH_TOKEN);

            // then
            assertThat(existing.getUser()).isSameAs(newOwner);
            assertThat(existing.getPlatform()).isEqualTo(DevicePlatform.WEB);
            assertThat(existing.isActive()).isTrue();
            verify(deviceTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("사용자 없으면 USER_NOT_FOUND를 던지고 토큰 조회를 스킵한다")
        void should_throwUserNotFound_when_userMissing() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(
                            () ->
                                    deviceTokenService.register(
                                            USER_ID, DevicePlatform.WEB, PUSH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(deviceTokenRepository);
        }
    }
}
