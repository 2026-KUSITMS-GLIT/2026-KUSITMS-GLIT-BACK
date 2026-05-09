package com.groute.groute_server.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.auth.repository.RefreshTokenRepository;
import com.groute.groute_server.record.application.port.out.star.StarImageStoragePort;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AccountHardDeleteServiceTest {

    private static final Long USER_ID = 1L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-09T03:00:00Z");

    @Mock UserRepository userRepository;
    @Mock StarImageStoragePort starImageStoragePort;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock AccountHardDeleteDbCleaner dbCleaner;
    @Mock Clock clock;

    @InjectMocks AccountHardDeleteService accountHardDeleteService;

    @BeforeEach
    void setUpClock() {
        // findExpiredUserIds만 clock을 사용. hardDelete 케이스는 reach 안 하므로 lenient 처리.
        lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("만료 사용자 조회 (findExpiredUserIds)")
    class FindExpiredUserIds {

        @Test
        @DisplayName("repo 결과가 비어 있으면 빈 리스트 반환")
        void should_returnEmpty_when_noExpiredUsers() {
            // given
            given(
                            userRepository.findExpiredHardDeleteUserIds(
                                    OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC)))
                    .willReturn(List.of());

            // when
            List<Long> result = accountHardDeleteService.findExpiredUserIds();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("repo 결과를 그대로 반환")
        void should_returnUserIds_when_expiredUsersExist() {
            // given
            given(
                            userRepository.findExpiredHardDeleteUserIds(
                                    OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC)))
                    .willReturn(List.of(1L, 2L, 3L));

            // when
            List<Long> result = accountHardDeleteService.findExpiredUserIds();

            // then
            assertThat(result).containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("주입된 Clock 시각으로 repo 호출 — 시스템 시계 직접 참조 안 함")
        void should_useInjectedClockTime_when_callingRepo() {
            // given
            given(
                            userRepository.findExpiredHardDeleteUserIds(
                                    OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC)))
                    .willReturn(List.of());

            // when
            accountHardDeleteService.findExpiredUserIds();

            // then
            verify(userRepository)
                    .findExpiredHardDeleteUserIds(
                            OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
        }
    }

    @Nested
    @DisplayName("회원 hard delete 실행 (hardDelete) - 정상")
    class HardDeleteHappyPath {

        @Test
        @DisplayName("storage → Redis → DB cleaner 순서로 호출된다")
        void should_invokeAllStepsInOrder_when_userValid() {
            // given
            // (모든 mock 기본 동작: noop)

            // when
            accountHardDeleteService.hardDelete(USER_ID);

            // then
            InOrder inOrder = inOrder(starImageStoragePort, refreshTokenRepository, dbCleaner);
            inOrder.verify(starImageStoragePort).deleteAllByUserId(USER_ID);
            inOrder.verify(refreshTokenRepository).deleteByUserId(USER_ID);
            inOrder.verify(dbCleaner).cascadeDelete(USER_ID);
        }
    }

    @Nested
    @DisplayName("회원 hard delete 실행 (hardDelete) - 예외")
    class HardDeleteErrors {

        @Test
        @DisplayName("storage 실패 시 예외 전파, Redis·DB cleaner 미호출")
        void should_propagateAndStop_when_storageFails() {
            // given
            willThrow(new RuntimeException("S3 down"))
                    .given(starImageStoragePort)
                    .deleteAllByUserId(USER_ID);

            // when & then
            assertThatThrownBy(() -> accountHardDeleteService.hardDelete(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("S3 down");
            verifyNoInteractions(refreshTokenRepository);
            verifyNoInteractions(dbCleaner);
        }

        @Test
        @DisplayName("Redis 실패 시 예외 전파, DB cleaner 미호출 (storage는 이미 호출됨)")
        void should_propagateAndStop_when_redisFails() {
            // given
            willThrow(new RuntimeException("redis down"))
                    .given(refreshTokenRepository)
                    .deleteByUserId(USER_ID);

            // when & then
            assertThatThrownBy(() -> accountHardDeleteService.hardDelete(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("redis down");
            verify(starImageStoragePort).deleteAllByUserId(USER_ID);
            verify(dbCleaner, never()).cascadeDelete(USER_ID);
        }

        @Test
        @DisplayName("DB cleaner 실패 시 예외 전파 (storage·Redis는 이미 호출됨, 다음 사이클에 재시도됨)")
        void should_propagate_when_dbCascadeFails() {
            // given
            willThrow(new RuntimeException("db down")).given(dbCleaner).cascadeDelete(USER_ID);

            // when & then
            assertThatThrownBy(() -> accountHardDeleteService.hardDelete(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db down");
            verify(starImageStoragePort).deleteAllByUserId(USER_ID);
            verify(refreshTokenRepository).deleteByUserId(USER_ID);
        }
    }
}
