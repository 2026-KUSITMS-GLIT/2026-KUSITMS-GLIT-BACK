package com.groute.groute_server.record.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.record.application.port.in.star.HomeSummaryResult;
import com.groute.groute_server.record.application.port.out.UserPort;
import com.groute.groute_server.record.domain.enums.ReportModalType;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class HomeSummaryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock UserPort userPort;

    @InjectMocks HomeSummaryService service;

    private User userWith(boolean coachMark, String modalType) {
        User user = User.createForSocialLogin();
        ReflectionTestUtils.setField(user, "pendingFirstStarCoachMark", coachMark);
        ReflectionTestUtils.setField(user, "pendingReportModalType", modalType);
        return user;
    }

    @Nested
    @DisplayName("isFirstStar")
    class IsFirstStar {

        @Test
        @DisplayName("pending_first_star_coach_mark=true이면 isFirstStar=true이고 소비 후 false로 초기화")
        void should_returnTrue_and_consume_when_pendingCoachMarkSet() {
            User user = userWith(true, null);
            given(userPort.findById(USER_ID)).willReturn(user);

            HomeSummaryResult result = service.getSummary(USER_ID);

            assertThat(result.isFirstStar()).isTrue();
            assertThat(user.isPendingFirstStarCoachMark()).isFalse();
        }

        @Test
        @DisplayName("pending_first_star_coach_mark=false이면 isFirstStar=false")
        void should_returnFalse_when_pendingCoachMarkNotSet() {
            given(userPort.findById(USER_ID)).willReturn(userWith(false, null));

            assertThat(service.getSummary(USER_ID).isFirstStar()).isFalse();
        }
    }

    @Nested
    @DisplayName("reportModal")
    class ReportModalTest {

        @Test
        @DisplayName("pending_report_modal_type=MINI이면 show=true, type=MINI이고 소비 후 null로 초기화")
        void should_returnMini_and_consume_when_pendingModalIsMini() {
            User user = userWith(false, "MINI");
            given(userPort.findById(USER_ID)).willReturn(user);

            HomeSummaryResult.ReportModal modal = service.getSummary(USER_ID).reportModal();

            assertThat(modal.show()).isTrue();
            assertThat(modal.type()).isEqualTo(ReportModalType.MINI);
            assertThat(user.getPendingReportModalType()).isNull();
        }

        @Test
        @DisplayName("pending_report_modal_type=FULL이면 show=true, type=FULL이고 소비 후 null로 초기화")
        void should_returnFull_and_consume_when_pendingModalIsFull() {
            User user = userWith(false, "FULL");
            given(userPort.findById(USER_ID)).willReturn(user);

            HomeSummaryResult.ReportModal modal = service.getSummary(USER_ID).reportModal();

            assertThat(modal.show()).isTrue();
            assertThat(modal.type()).isEqualTo(ReportModalType.FULL);
            assertThat(user.getPendingReportModalType()).isNull();
        }

        @Test
        @DisplayName("pending_report_modal_type=null이면 show=false, type=null")
        void should_returnNone_when_pendingModalNotSet() {
            given(userPort.findById(USER_ID)).willReturn(userWith(false, null));

            HomeSummaryResult.ReportModal modal = service.getSummary(USER_ID).reportModal();

            assertThat(modal.show()).isFalse();
            assertThat(modal.type()).isNull();
        }
    }

    @Nested
    @DisplayName("isFirstStar + reportModal 조합")
    class CombinationTest {

        @Test
        @DisplayName("코치마크와 모달 둘 다 pending이면 둘 다 반환")
        void should_returnBoth_when_bothPending() {
            given(userPort.findById(USER_ID)).willReturn(userWith(true, "MINI"));

            HomeSummaryResult result = service.getSummary(USER_ID);

            assertThat(result.isFirstStar()).isTrue();
            assertThat(result.reportModal().show()).isTrue();
            assertThat(result.reportModal().type()).isEqualTo(ReportModalType.MINI);
        }

        @Test
        @DisplayName("둘 다 pending 없으면 isFirstStar=false, modal show=false")
        void should_returnNone_when_nothingPending() {
            given(userPort.findById(USER_ID)).willReturn(userWith(false, null));

            HomeSummaryResult result = service.getSummary(USER_ID);

            assertThat(result.isFirstStar()).isFalse();
            assertThat(result.reportModal().show()).isFalse();
        }
    }
}
