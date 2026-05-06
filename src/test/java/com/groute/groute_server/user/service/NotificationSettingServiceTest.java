package com.groute.groute_server.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.user.dto.NotificationSettingsUpdateRequest;
import com.groute.groute_server.user.entity.NotificationSetting;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.enums.DayOfWeek;
import com.groute.groute_server.user.repository.NotificationSettingRepository;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalTime NOTIFY_TIME = LocalTime.of(9, 0);

    @Mock NotificationSettingRepository notificationSettingRepository;
    @Mock UserRepository userRepository;
    @Mock EntityManager entityManager;

    @InjectMocks NotificationSettingService notificationSettingService;

    @Nested
    @DisplayName("정상 교체")
    class HappyPath {

        @Test
        @DisplayName("요일 N개 + active=true면 모든 row가 동일 notifyTime/active로 saveAll된다")
        void should_saveAllSlotsWithSameTimeAndActive_when_validRequest() {
            // given
            NotificationSettingsUpdateRequest request =
                    new NotificationSettingsUpdateRequest(
                            true, List.of(DayOfWeek.MON, DayOfWeek.TUE), NOTIFY_TIME);
            User userRef = User.createForSocialLogin();
            given(userRepository.existsById(USER_ID)).willReturn(true);
            given(userRepository.getReferenceById(USER_ID)).willReturn(userRef);

            // when
            notificationSettingService.replaceMySettings(USER_ID, request);

            // then — delete → flush → clear → saveAll 순서 검증
            InOrder order = inOrder(notificationSettingRepository, entityManager);
            order.verify(notificationSettingRepository).deleteAllByUser_Id(USER_ID);
            order.verify(entityManager).flush();
            order.verify(entityManager).clear();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<NotificationSetting>> captor = ArgumentCaptor.forClass(List.class);
            order.verify(notificationSettingRepository).saveAll(captor.capture());
            List<NotificationSetting> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved)
                    .allSatisfy(
                            s -> {
                                assertThat(s.getUser()).isSameAs(userRef);
                                assertThat(s.getNotifyTime()).isEqualTo(NOTIFY_TIME);
                                assertThat(s.isActive()).isTrue();
                            });
            assertThat(saved)
                    .extracting(NotificationSetting::getDayOfWeek)
                    .containsExactlyInAnyOrder(DayOfWeek.MON, DayOfWeek.TUE);
        }

        @Test
        @DisplayName("빈 days + active=false면 deleteAll만 호출하고 saveAll/getReferenceById는 호출하지 않는다")
        void should_onlyDeleteAll_when_emptyDaysWithInactive() {
            // given
            NotificationSettingsUpdateRequest request =
                    new NotificationSettingsUpdateRequest(false, List.of(), NOTIFY_TIME);
            given(userRepository.existsById(USER_ID)).willReturn(true);

            // when
            notificationSettingService.replaceMySettings(USER_ID, request);

            // then
            InOrder order = inOrder(notificationSettingRepository, entityManager);
            order.verify(notificationSettingRepository).deleteAllByUser_Id(USER_ID);
            order.verify(entityManager).flush();
            order.verify(entityManager).clear();
            verify(notificationSettingRepository, never()).saveAll(any());
            verify(userRepository, never()).getReferenceById(anyLong());
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("daysOfWeek 중복이 있으면 DUPLICATE_NOTIFICATION_SLOT을 던지고 어떤 쓰기도 하지 않는다")
        void should_throwDuplicateNotificationSlot_when_daysHaveDuplicate() {
            // given
            NotificationSettingsUpdateRequest request =
                    new NotificationSettingsUpdateRequest(
                            true, List.of(DayOfWeek.MON, DayOfWeek.MON), NOTIFY_TIME);

            // when & then
            assertThatThrownBy(() -> notificationSettingService.replaceMySettings(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_NOTIFICATION_SLOT);
            verifyNoInteractions(notificationSettingRepository, userRepository, entityManager);
        }

        @Test
        @DisplayName("빈 days + active=true면 NOTIFICATION_DAYS_REQUIRED를 던지고 어떤 쓰기도 하지 않는다")
        void should_throwNotificationDaysRequired_when_emptyDaysWithActive() {
            // given
            NotificationSettingsUpdateRequest request =
                    new NotificationSettingsUpdateRequest(true, List.of(), NOTIFY_TIME);

            // when & then
            assertThatThrownBy(() -> notificationSettingService.replaceMySettings(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOTIFICATION_DAYS_REQUIRED);
            verifyNoInteractions(notificationSettingRepository, userRepository, entityManager);
        }

        @Test
        @DisplayName("사용자 없으면 USER_NOT_FOUND를 던지고 슬롯 변경/엔티티매니저는 건드리지 않는다")
        void should_throwUserNotFound_when_userMissing() {
            // given
            NotificationSettingsUpdateRequest request =
                    new NotificationSettingsUpdateRequest(
                            true, List.of(DayOfWeek.MON), NOTIFY_TIME);
            given(userRepository.existsById(USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> notificationSettingService.replaceMySettings(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(notificationSettingRepository, entityManager);
        }
    }
}
