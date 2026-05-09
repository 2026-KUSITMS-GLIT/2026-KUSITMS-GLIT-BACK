package com.groute.groute_server.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.auth.repository.DeviceTokenRepository;
import com.groute.groute_server.auth.repository.SocialAccountRepository;
import com.groute.groute_server.auth.repository.UserTermAgreementRepository;
import com.groute.groute_server.record.application.port.out.RecordHardDeletePort;
import com.groute.groute_server.report.application.port.out.ReportHardDeletePort;
import com.groute.groute_server.user.repository.CoachmarkHistoryRepository;
import com.groute.groute_server.user.repository.NotificationSettingRepository;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AccountHardDeleteDbCleanerTest {

    private static final Long USER_ID = 1L;

    @Mock RecordHardDeletePort recordHardDeletePort;
    @Mock ReportHardDeletePort reportHardDeletePort;
    @Mock NotificationSettingRepository notificationSettingRepository;
    @Mock CoachmarkHistoryRepository coachmarkHistoryRepository;
    @Mock DeviceTokenRepository deviceTokenRepository;
    @Mock SocialAccountRepository socialAccountRepository;
    @Mock UserTermAgreementRepository userTermAgreementRepository;
    @Mock UserRepository userRepository;

    @InjectMocks AccountHardDeleteDbCleaner cleaner;

    @Nested
    @DisplayName("DB cascade 삭제 - 정상")
    class HappyPath {

        @Test
        @DisplayName("FK 의존 순서대로 호출: record → report → user/auth Layered → users row")
        void should_invokeAllPortsInFkSafeOrder_when_userIdGiven() {
            // given (모든 mock 기본 동작: noop)

            // when
            cleaner.cascadeDelete(USER_ID);

            // then — InOrder로 호출 순서 정확히 검증
            InOrder inOrder =
                    inOrder(
                            recordHardDeletePort,
                            reportHardDeletePort,
                            notificationSettingRepository,
                            coachmarkHistoryRepository,
                            deviceTokenRepository,
                            socialAccountRepository,
                            userTermAgreementRepository,
                            userRepository);
            inOrder.verify(recordHardDeletePort).hardDeleteAllByUserId(USER_ID);
            inOrder.verify(reportHardDeletePort).hardDeleteAllByUserId(USER_ID);
            inOrder.verify(notificationSettingRepository).hardDeleteAllByUserId(USER_ID);
            inOrder.verify(coachmarkHistoryRepository).hardDeleteAllByUserId(USER_ID);
            inOrder.verify(deviceTokenRepository).hardDeleteAllByUserId(USER_ID);
            inOrder.verify(socialAccountRepository).hardDeleteAllByUserId(USER_ID);
            inOrder.verify(userTermAgreementRepository).hardDeleteAllByUserId(USER_ID);
            inOrder.verify(userRepository).deleteById(USER_ID);
        }

        @Test
        @DisplayName("users row 삭제는 cascade 끝에서만 호출 (FK 자식 정리 후)")
        void should_callDeleteByIdLast_when_userIdGiven() {
            // when
            cleaner.cascadeDelete(USER_ID);

            // then
            // users 삭제 호출 시점에 다른 모든 자식 삭제는 이미 완료되어야 한다.
            InOrder inOrder = inOrder(deviceTokenRepository, userRepository);
            inOrder.verify(deviceTokenRepository).hardDeleteAllByUserId(USER_ID);
            inOrder.verify(userRepository).deleteById(USER_ID);
        }
    }

    @Nested
    @DisplayName("DB cascade 삭제 - 예외")
    class Errors {

        @Test
        @DisplayName("첫 단계(record port) 실패 시 후속 단계 미호출 — 트랜잭션 롤백 호출자 책임")
        void should_stopAndPropagate_when_firstStepFails() {
            // given
            willThrow(new RuntimeException("record fail"))
                    .given(recordHardDeletePort)
                    .hardDeleteAllByUserId(USER_ID);

            // when & then
            assertThatThrownBy(() -> cleaner.cascadeDelete(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("record fail");
            verify(reportHardDeletePort, never()).hardDeleteAllByUserId(USER_ID);
            verify(notificationSettingRepository, never()).hardDeleteAllByUserId(USER_ID);
            verify(coachmarkHistoryRepository, never()).hardDeleteAllByUserId(USER_ID);
            verify(deviceTokenRepository, never()).hardDeleteAllByUserId(USER_ID);
            verify(socialAccountRepository, never()).hardDeleteAllByUserId(USER_ID);
            verify(userTermAgreementRepository, never()).hardDeleteAllByUserId(USER_ID);
            verify(userRepository, never()).deleteById(USER_ID);
        }

        @Test
        @DisplayName("중간 단계(deviceToken) 실패 시 이전 단계는 호출, 이후는 미호출")
        void should_haltCascade_when_midStepFails() {
            // given
            willThrow(new RuntimeException("device fail"))
                    .given(deviceTokenRepository)
                    .hardDeleteAllByUserId(USER_ID);

            // when & then
            assertThatThrownBy(() -> cleaner.cascadeDelete(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("device fail");
            // 이전 단계는 모두 호출됐어야 함
            verify(recordHardDeletePort).hardDeleteAllByUserId(USER_ID);
            verify(reportHardDeletePort).hardDeleteAllByUserId(USER_ID);
            verify(notificationSettingRepository).hardDeleteAllByUserId(USER_ID);
            verify(coachmarkHistoryRepository).hardDeleteAllByUserId(USER_ID);
            // 이후 단계는 모두 미호출
            verify(socialAccountRepository, never()).hardDeleteAllByUserId(USER_ID);
            verify(userTermAgreementRepository, never()).hardDeleteAllByUserId(USER_ID);
            verify(userRepository, never()).deleteById(USER_ID);
        }

        @Test
        @DisplayName("마지막 단계(users delete) 실패 시 이전 단계는 모두 호출 — 트랜잭션 롤백으로 자식 삭제도 되돌아감")
        void should_propagate_when_userDeleteFails() {
            // given
            willThrow(new RuntimeException("user delete fail"))
                    .given(userRepository)
                    .deleteById(USER_ID);

            // when & then
            assertThatThrownBy(() -> cleaner.cascadeDelete(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("user delete fail");
            verify(recordHardDeletePort).hardDeleteAllByUserId(USER_ID);
            verify(reportHardDeletePort).hardDeleteAllByUserId(USER_ID);
            verify(notificationSettingRepository).hardDeleteAllByUserId(USER_ID);
            verify(coachmarkHistoryRepository).hardDeleteAllByUserId(USER_ID);
            verify(deviceTokenRepository).hardDeleteAllByUserId(USER_ID);
            verify(socialAccountRepository).hardDeleteAllByUserId(USER_ID);
            verify(userTermAgreementRepository).hardDeleteAllByUserId(USER_ID);
        }
    }
}
