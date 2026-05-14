package com.groute.groute_server.user.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.user.service.AccountHardDeleteService;

@ExtendWith(MockitoExtension.class)
class AccountHardDeleteSchedulerTest {

    @Mock AccountHardDeleteService accountHardDeleteService;

    @InjectMocks AccountHardDeleteScheduler scheduler;

    @Nested
    @DisplayName("일일 dispatch 사이클 - 정상")
    class HappyPath {

        @Test
        @DisplayName("대상 사용자 목록 각각에 대해 hardDelete를 정확히 1회씩 호출")
        void should_callHardDeleteForEachUser_when_targetsExist() {
            // given
            given(accountHardDeleteService.findExpiredUserIds()).willReturn(List.of(1L, 2L, 3L));

            // when
            scheduler.dispatch();

            // then
            verify(accountHardDeleteService).hardDelete(1L);
            verify(accountHardDeleteService).hardDelete(2L);
            verify(accountHardDeleteService).hardDelete(3L);
        }

        @Test
        @DisplayName("대상 없으면 hardDelete 미호출 (early return)")
        void should_skipDispatch_when_noExpiredUsers() {
            // given
            given(accountHardDeleteService.findExpiredUserIds()).willReturn(List.of());

            // when
            scheduler.dispatch();

            // then
            verify(accountHardDeleteService).findExpiredUserIds();
            verifyNoMoreInteractions(accountHardDeleteService);
        }

        @Test
        @DisplayName("단일 사용자 케이스도 정상 처리")
        void should_handleSingleUser_when_oneTarget() {
            // given
            given(accountHardDeleteService.findExpiredUserIds()).willReturn(List.of(42L));

            // when
            scheduler.dispatch();

            // then
            verify(accountHardDeleteService).hardDelete(42L);
        }
    }

    @Nested
    @DisplayName("일일 dispatch 사이클 - 실패 격리")
    class FailureIsolation {

        @Test
        @DisplayName("한 사용자가 실패해도 다음 사용자는 정상 처리")
        void should_continueProcessing_when_oneUserFails() {
            // given
            given(accountHardDeleteService.findExpiredUserIds()).willReturn(List.of(1L, 2L, 3L));
            willThrow(new RuntimeException("user 2 fail"))
                    .given(accountHardDeleteService)
                    .hardDelete(2L);

            // when (예외가 dispatch 밖으로 새지 않아야 함)
            scheduler.dispatch();

            // then — 실패한 user 2도 호출은 됐고, 이후 user 3도 호출됨
            verify(accountHardDeleteService).hardDelete(1L);
            verify(accountHardDeleteService).hardDelete(2L);
            verify(accountHardDeleteService).hardDelete(3L);
        }

        @Test
        @DisplayName("모든 사용자가 실패해도 전체 사이클은 끝까지 진행 (다음 스케줄 트리거 정상화)")
        void should_completeCycle_when_allUsersFail() {
            // given
            given(accountHardDeleteService.findExpiredUserIds()).willReturn(List.of(1L, 2L, 3L));
            willThrow(new RuntimeException("blanket fail"))
                    .given(accountHardDeleteService)
                    .hardDelete(1L);
            willThrow(new RuntimeException("blanket fail"))
                    .given(accountHardDeleteService)
                    .hardDelete(2L);
            willThrow(new RuntimeException("blanket fail"))
                    .given(accountHardDeleteService)
                    .hardDelete(3L);

            // when (모든 사용자 실패해도 dispatch는 정상 종료)
            scheduler.dispatch();

            // then — 모든 사용자에 대해 시도는 됐어야 함
            verify(accountHardDeleteService, times(3))
                    .hardDelete(org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("첫 사용자 실패 시 두 번째 hardDelete 호출 보장")
        void should_callSecondUser_when_firstFails() {
            // given
            given(accountHardDeleteService.findExpiredUserIds()).willReturn(List.of(1L, 2L));
            willThrow(new RuntimeException("first fail"))
                    .given(accountHardDeleteService)
                    .hardDelete(1L);

            // when
            scheduler.dispatch();

            // then
            verify(accountHardDeleteService).hardDelete(1L);
            verify(accountHardDeleteService).hardDelete(2L);
        }

        @Test
        @DisplayName("findExpiredUserIds 자체가 실패하면 dispatch 중단 + 예외 전파")
        void should_propagate_when_findExpiredFails() {
            // given
            willThrow(new RuntimeException("query fail"))
                    .given(accountHardDeleteService)
                    .findExpiredUserIds();

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> scheduler.dispatch())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("query fail");
            verify(accountHardDeleteService, never())
                    .hardDelete(org.mockito.ArgumentMatchers.anyLong());
        }
    }
}
