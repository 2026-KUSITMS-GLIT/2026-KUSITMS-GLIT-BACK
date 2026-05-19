package com.groute.groute_server.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.auth.enums.DevicePlatform;
import com.groute.groute_server.auth.repository.DeviceTokenRepository;
import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
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
        @DisplayName("native upsert를 platform name·pushToken과 함께 호출한다 (insert/update 분기는 DB가 처리)")
        void should_callUpsert_when_userExists() {
            // given
            given(userRepository.existsById(USER_ID)).willReturn(true);

            // when
            deviceTokenService.register(USER_ID, DevicePlatform.WEB, PUSH_TOKEN);

            // then
            verify(deviceTokenRepository)
                    .upsertByPushToken(USER_ID, DevicePlatform.WEB.name(), PUSH_TOKEN);
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("사용자 없으면 USER_NOT_FOUND를 던지고 토큰 저장소는 건드리지 않는다")
        void should_throwUserNotFound_when_userMissing() {
            // given
            given(userRepository.existsById(USER_ID)).willReturn(false);

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
