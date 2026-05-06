package com.groute.groute_server.common.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.auth.entity.DeviceToken;
import com.groute.groute_server.auth.enums.DevicePlatform;
import com.groute.groute_server.auth.repository.DeviceTokenRepository;
import com.groute.groute_server.common.notification.copy.NotificationCopy;
import com.groute.groute_server.common.notification.copy.NotificationCopyProperties;
import com.groute.groute_server.common.notification.fcm.client.FcmPushClient;
import com.groute.groute_server.common.notification.fcm.model.FcmPayload;
import com.groute.groute_server.common.notification.fcm.model.SendResult;
import com.groute.groute_server.record.application.service.ScrumDailyQueryService;
import com.groute.groute_server.user.entity.NotificationSetting;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.enums.DayOfWeek;
import com.groute.groute_server.user.repository.NotificationSettingRepository;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 2026-01-06 화요일 09:00 KST. UTC로는 2026-01-06 00:00. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(
                    ZonedDateTime.of(LocalDate.of(2026, 1, 6), LocalTime.of(9, 0), KST).toInstant(),
                    ZoneOffset.UTC);

    private static final String DEEP_LINK = "https://app.glit.today/scrum/new";

    @Mock NotificationSettingRepository notificationSettingRepository;
    @Mock DeviceTokenRepository deviceTokenRepository;
    @Mock UserRepository userRepository;
    @Mock FcmPushClient fcmPushClient;
    @Mock ScrumDailyQueryService scrumDailyQueryService;

    NotificationScheduler scheduler;
    NotificationCopy notificationCopy;
    NotificationCopyProperties properties;

    /**
     * Clock.fixed와 NotificationCopy/Properties record는 mock으로 다루기 부적합해 매 테스트마다 신선한 인스턴스로 수동 와이어링.
     * (다른 *ServiceTest는 @InjectMocks 사용, 본 테스트는 Clock 주입 제약으로 예외)
     */
    @BeforeEach
    void setUp() {
        notificationCopy =
                new NotificationCopy(
                        List.of(
                                new NotificationCopy.Item("A {닉네임}", "본문 A"),
                                new NotificationCopy.Item("B {닉네임}", "본문 B"),
                                new NotificationCopy.Item("C {닉네임}", "본문 C")));
        properties = new NotificationCopyProperties("ignored", DEEP_LINK);
        scheduler =
                new NotificationScheduler(
                        notificationSettingRepository,
                        deviceTokenRepository,
                        userRepository,
                        fcmPushClient,
                        notificationCopy,
                        properties,
                        scrumDailyQueryService,
                        FIXED_CLOCK);
    }

    @Nested
    @DisplayName("정상 발송")
    class HappyPath {

        @Test
        @DisplayName("KST 09:00 화요일 매칭 시 카피를 닉네임 치환·딥링크와 함께 발송하고 카피 인덱스를 advance한다")
        void should_sendWithNicknameAndLinkAndAdvanceIndex_when_matchedSlot() {
            // given
            User user = user(1L, "겨레", (short) 0);
            given(
                            notificationSettingRepository
                                    .findAllByDayOfWeekAndNotifyTimeAndIsActiveTrue(
                                            DayOfWeek.TUE, LocalTime.of(9, 0)))
                    .willReturn(List.of(slot(user)));
            given(
                            scrumDailyQueryService.findUserIdsWithScrumOn(
                                    eq(LocalDate.of(2026, 1, 6)), any()))
                    .willReturn(Set.of());
            given(userRepository.findAllById(any())).willReturn(List.of(user));
            given(deviceTokenRepository.findAllByUser_IdInAndIsActiveTrue(any()))
                    .willReturn(List.of(token(user, "tok-1")));
            given(fcmPushClient.send(any(), any())).willReturn(new SendResult(true, false));

            // when
            scheduler.dispatch();

            // then
            ArgumentCaptor<FcmPayload> captor = ArgumentCaptor.forClass(FcmPayload.class);
            verify(fcmPushClient).send(eq("tok-1"), captor.capture());
            FcmPayload payload = captor.getValue();
            assertThat(payload.title()).isEqualTo("A 겨레");
            assertThat(payload.body()).isEqualTo("본문 A");
            assertThat(payload.link()).isEqualTo(DEEP_LINK);
            assertThat(user.getNotificationCopyIndex()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("user별 카피 인덱스에 따라 라운드로빈으로 카피를 선택하고 mod로 advance한다 (0→1, 1→2, 2→0)")
        void should_pickCopyByIndexAndAdvanceWithMod_when_multipleUsers() {
            // given — copy_index 0/1/2인 3 users
            User u1 = user(1L, "유저1", (short) 0);
            User u2 = user(2L, "유저2", (short) 1);
            User u3 = user(3L, "유저3", (short) 2);
            given(
                            notificationSettingRepository
                                    .findAllByDayOfWeekAndNotifyTimeAndIsActiveTrue(any(), any()))
                    .willReturn(List.of(slot(u1), slot(u2), slot(u3)));
            given(scrumDailyQueryService.findUserIdsWithScrumOn(any(), any())).willReturn(Set.of());
            given(userRepository.findAllById(any())).willReturn(List.of(u1, u2, u3));
            given(deviceTokenRepository.findAllByUser_IdInAndIsActiveTrue(any()))
                    .willReturn(List.of(token(u1, "t1"), token(u2, "t2"), token(u3, "t3")));
            given(fcmPushClient.send(any(), any())).willReturn(new SendResult(true, false));

            // when
            scheduler.dispatch();

            // then — token별 어떤 카피가 갔는지 매핑
            ArgumentCaptor<String> tokenCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<FcmPayload> payloadCap = ArgumentCaptor.forClass(FcmPayload.class);
            verify(fcmPushClient, times(3)).send(tokenCap.capture(), payloadCap.capture());
            Map<String, FcmPayload> sent = new HashMap<>();
            for (int i = 0; i < tokenCap.getAllValues().size(); i++) {
                sent.put(tokenCap.getAllValues().get(i), payloadCap.getAllValues().get(i));
            }
            assertThat(sent.get("t1").title()).isEqualTo("A 유저1"); // idx=0 → 카피 A
            assertThat(sent.get("t2").title()).isEqualTo("B 유저2"); // idx=1 → 카피 B
            assertThat(sent.get("t3").title()).isEqualTo("C 유저3"); // idx=2 → 카피 C
            // mod 동작: 0+1=1, 1+1=2, 2+1=3 mod 3 = 0
            assertThat(u1.getNotificationCopyIndex()).isEqualTo((short) 1);
            assertThat(u2.getNotificationCopyIndex()).isEqualTo((short) 2);
            assertThat(u3.getNotificationCopyIndex()).isEqualTo((short) 0);
        }

        @Test
        @DisplayName("한 user에 토큰 N개면 모두 발송하지만 카피 인덱스는 1번만 advance한다")
        void should_sendToAllTokensButAdvanceIndexOnce_when_userHasMultipleTokens() {
            // given
            User user = user(1L, "겨레", (short) 0);
            given(
                            notificationSettingRepository
                                    .findAllByDayOfWeekAndNotifyTimeAndIsActiveTrue(any(), any()))
                    .willReturn(List.of(slot(user)));
            given(scrumDailyQueryService.findUserIdsWithScrumOn(any(), any())).willReturn(Set.of());
            given(userRepository.findAllById(any())).willReturn(List.of(user));
            given(deviceTokenRepository.findAllByUser_IdInAndIsActiveTrue(any()))
                    .willReturn(List.of(token(user, "t1"), token(user, "t2"), token(user, "t3")));
            given(fcmPushClient.send(any(), any())).willReturn(new SendResult(true, false));

            // when
            scheduler.dispatch();

            // then
            verify(fcmPushClient).send(eq("t1"), any());
            verify(fcmPushClient).send(eq("t2"), any());
            verify(fcmPushClient).send(eq("t3"), any());
            assertThat(user.getNotificationCopyIndex()).isEqualTo((short) 1);
        }
    }

    @Nested
    @DisplayName("필터링·토큰 정리")
    class Filtering {

        @Test
        @DisplayName("당일 작성자(user 2)는 발송에서 제외되고 카피 인덱스도 변하지 않는다")
        void should_skipWriters_when_someUsersAlreadyWrote() {
            // given — 후보 {1,2,3}, 작성자 {2}
            User u1 = user(1L, "U1", (short) 0);
            User u2 = user(2L, "U2", (short) 0);
            User u3 = user(3L, "U3", (short) 0);
            given(
                            notificationSettingRepository
                                    .findAllByDayOfWeekAndNotifyTimeAndIsActiveTrue(any(), any()))
                    .willReturn(List.of(slot(u1), slot(u2), slot(u3)));
            given(scrumDailyQueryService.findUserIdsWithScrumOn(any(), any()))
                    .willReturn(Set.of(2L));
            // 서비스가 candidate에서 작성자 제외 후 user/토큰 조회 (u2 빠짐)
            given(userRepository.findAllById(any())).willReturn(List.of(u1, u3));
            given(deviceTokenRepository.findAllByUser_IdInAndIsActiveTrue(any()))
                    .willReturn(List.of(token(u1, "t1"), token(u3, "t3")));
            given(fcmPushClient.send(any(), any())).willReturn(new SendResult(true, false));

            // when
            scheduler.dispatch();

            // then
            verify(fcmPushClient).send(eq("t1"), any());
            verify(fcmPushClient).send(eq("t3"), any());
            verify(fcmPushClient, never()).send(eq("t2"), any());
            assertThat(u2.getNotificationCopyIndex()).isEqualTo((short) 0);
        }

        @Test
        @DisplayName("invalid 응답을 받은 토큰만 deactivateByPushToken으로 비활성화된다")
        void should_deactivateOnlyInvalidTokens_when_fcmReturnsTokenInvalid() {
            // given
            User user = user(1L, "겨레", (short) 0);
            DeviceToken good = token(user, "t-good");
            DeviceToken bad = token(user, "t-bad");
            given(
                            notificationSettingRepository
                                    .findAllByDayOfWeekAndNotifyTimeAndIsActiveTrue(any(), any()))
                    .willReturn(List.of(slot(user)));
            given(scrumDailyQueryService.findUserIdsWithScrumOn(any(), any())).willReturn(Set.of());
            given(userRepository.findAllById(any())).willReturn(List.of(user));
            given(deviceTokenRepository.findAllByUser_IdInAndIsActiveTrue(any()))
                    .willReturn(List.of(good, bad));
            given(fcmPushClient.send(eq("t-good"), any())).willReturn(new SendResult(true, false));
            given(fcmPushClient.send(eq("t-bad"), any())).willReturn(new SendResult(false, true));

            // when
            scheduler.dispatch();

            // then
            verify(deviceTokenRepository).deactivateByPushToken("t-bad");
            verify(deviceTokenRepository, never()).deactivateByPushToken("t-good");
        }
    }

    // ============== helpers ==============

    private static User user(Long id, String nickname, short copyIndex) {
        try {
            Constructor<User> ctor = User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            User user = ctor.newInstance();
            ReflectionTestUtils.setField(user, "id", id);
            ReflectionTestUtils.setField(user, "nickname", nickname);
            ReflectionTestUtils.setField(user, "notificationCopyIndex", copyIndex);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static NotificationSetting slot(User user) {
        return NotificationSetting.create(user, DayOfWeek.TUE, LocalTime.of(9, 0), true);
    }

    private static DeviceToken token(User user, String pushToken) {
        return DeviceToken.register(user, DevicePlatform.WEB, pushToken);
    }
}
