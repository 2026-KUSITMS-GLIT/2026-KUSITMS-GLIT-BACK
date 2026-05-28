package com.groute.groute_server.record.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groute.groute_server.record.application.port.out.UserPort;
import com.groute.groute_server.record.application.port.out.star.StarRecordRepositoryPort;
import com.groute.groute_server.record.application.service.UserMilestoneTracker;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class UserMilestoneTrackerTest {

    private static final long USER_ID = 1L;

    @Mock StarRecordRepositoryPort starRecordPort;
    @Mock UserPort userPort;

    @InjectMocks UserMilestoneTracker userMilestoneTracker;

    @Nested
    @DisplayName("HappyPath")
    class HappyPath {

        @Test
        @DisplayName("성공 — 1번째 태깅이면 코치마크 노출 예약")
        void schedulesCoachMark_whenFirstTagging() {
            // given
            User user = User.createForSocialLogin();
            given(starRecordPort.countTaggedByUserId(USER_ID)).willReturn(1L);
            given(userPort.findById(USER_ID)).willReturn(user);

            // when
            userMilestoneTracker.track(USER_ID);

            // then
            assertThat(user.isPendingFirstStarCoachMark()).isTrue();
            assertThat(user.getPendingReportModalType()).isNull();
        }

        @Test
        @DisplayName("성공 — 10번째 태깅이면 MINI 리포트 모달 노출 예약")
        void schedulesMiniReportModal_whenCountIs10() {
            // given
            User user = User.createForSocialLogin();
            given(starRecordPort.countTaggedByUserId(USER_ID)).willReturn(10L);
            given(userPort.findById(USER_ID)).willReturn(user);

            // when
            userMilestoneTracker.track(USER_ID);

            // then
            assertThat(user.getPendingReportModalType()).isEqualTo("MINI");
        }

        @Test
        @DisplayName("성공 — 20번째 태깅이면 FULL 리포트 모달 노출 예약")
        void schedulesFullReportModal_whenCountIs20() {
            // given
            User user = User.createForSocialLogin();
            given(starRecordPort.countTaggedByUserId(USER_ID)).willReturn(20L);
            given(userPort.findById(USER_ID)).willReturn(user);

            // when
            userMilestoneTracker.track(USER_ID);

            // then
            assertThat(user.getPendingReportModalType()).isEqualTo("FULL");
        }

        @Test
        @DisplayName("성공 — 30번째 태깅이면 FULL 리포트 모달 노출 예약")
        void schedulesFullReportModal_whenCountIs30() {
            // given
            User user = User.createForSocialLogin();
            given(starRecordPort.countTaggedByUserId(USER_ID)).willReturn(30L);
            given(userPort.findById(USER_ID)).willReturn(user);

            // when
            userMilestoneTracker.track(USER_ID);

            // then
            assertThat(user.getPendingReportModalType()).isEqualTo("FULL");
        }
    }

    @Nested
    @DisplayName("Skips")
    class Skips {

        @Test
        @DisplayName("스킵 — 마일스톤이 아니면 아무 예약도 하지 않음")
        void skips_whenCountNotMilestone() {
            // given
            User user = User.createForSocialLogin();
            given(starRecordPort.countTaggedByUserId(USER_ID)).willReturn(5L);
            given(userPort.findById(USER_ID)).willReturn(user);

            // when
            userMilestoneTracker.track(USER_ID);

            // then
            assertThat(user.isPendingFirstStarCoachMark()).isFalse();
            assertThat(user.getPendingReportModalType()).isNull();
        }

        @Test
        @DisplayName("스킵 — 20 이상이어도 10의 배수가 아니면 모달 예약 안 함")
        void skips_whenCountIsNotMultipleOf10AboveBase() {
            // given
            User user = User.createForSocialLogin();
            given(starRecordPort.countTaggedByUserId(USER_ID)).willReturn(25L);
            given(userPort.findById(USER_ID)).willReturn(user);

            // when
            userMilestoneTracker.track(USER_ID);

            // then
            assertThat(user.getPendingReportModalType()).isNull();
        }
    }
}
