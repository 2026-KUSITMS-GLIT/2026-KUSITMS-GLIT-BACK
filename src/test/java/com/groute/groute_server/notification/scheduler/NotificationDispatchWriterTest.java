package com.groute.groute_server.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.auth.repository.DeviceTokenRepository;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchWriterTest {

    @Mock UserRepository userRepository;
    @Mock DeviceTokenRepository deviceTokenRepository;

    @InjectMocks NotificationDispatchWriter writer;

    private static User user(Long id, short copyIndex) {
        try {
            Constructor<User> ctor = User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            User user = ctor.newInstance();
            ReflectionTestUtils.setField(user, "id", id);
            ReflectionTestUtils.setField(user, "notificationCopyIndex", copyIndex);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nested
    @DisplayName("HappyPath")
    class HappyPath {

        @Test
        @DisplayName("성공 — 처리된 user는 copyPoolSize로 advance하고 invalid 토큰은 비활성화한다")
        void advancesUserIndexAndDeactivatesTokens() {
            // given
            User u1 = user(1L, (short) 0);
            User u2 = user(2L, (short) 2);
            given(userRepository.findAllById(Set.of(1L, 2L))).willReturn(List.of(u1, u2));

            // when
            writer.apply(Set.of(1L, 2L), 3, List.of("bad-1", "bad-2"));

            // then
            verify(deviceTokenRepository).deactivateByPushToken("bad-1");
            verify(deviceTokenRepository).deactivateByPushToken("bad-2");
            assertThat(u1.getNotificationCopyIndex()).isEqualTo((short) 1);
            assertThat(u2.getNotificationCopyIndex()).isEqualTo((short) 0); // 2+1 = 3 mod 3 = 0
        }

        @Test
        @DisplayName("성공 — invalid 토큰만 있고 처리된 user가 없으면 비활성화만 수행한다")
        void onlyDeactivates_whenNoProcessedUsers() {
            // when
            writer.apply(Set.of(), 3, List.of("bad-1"));

            // then
            verify(deviceTokenRepository).deactivateByPushToken("bad-1");
            verify(userRepository, never()).findAllById(any());
        }
    }

    @Nested
    @DisplayName("Skips")
    class Skips {

        @Test
        @DisplayName("스킵 — copyPoolSize가 0이면 advance를 수행하지 않는다")
        void skipsAdvance_whenCopyPoolSizeIsZero() {
            // when
            writer.apply(Set.of(1L), 0, List.of());

            // then
            verify(userRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("스킵 — invalid 토큰이 없고 처리된 user도 없으면 아무 일도 안 한다")
        void noop_whenAllInputsEmpty() {
            // when
            writer.apply(Set.of(), 3, List.of());

            // then
            verify(userRepository, never()).findAllById(any());
            verify(deviceTokenRepository, never()).deactivateByPushToken(any());
        }
    }
}
