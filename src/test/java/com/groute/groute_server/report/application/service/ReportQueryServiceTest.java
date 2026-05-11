package com.groute.groute_server.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.report.adapter.in.web.dto.ReportDetailResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportGaugeResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportListResponse;
import com.groute.groute_server.report.application.port.out.ReportQueryPort;
import com.groute.groute_server.report.application.port.out.StarRecordCountQueryPort;
import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportStatus;
import com.groute.groute_server.report.domain.enums.ReportType;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class ReportQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long REPORT_ID = 10L;

    @Mock ReportQueryPort reportQueryPort;
    @Mock StarRecordCountQueryPort starRecordCountQueryPort;

    @InjectMocks ReportQueryService service;

    // ============================================================
    // getGauge
    // ============================================================
    @Nested
    @DisplayName("getGauge")
    class GetGauge {

        @Test
        @DisplayName("신규 유저: 리포트 없으면 전체 완료 심화기록 수를 currentCount로 반환한다")
        void should_returnTotalCount_when_noReport() {
            // given
            given(reportQueryPort.findLatestSuccessByUserId(USER_ID)).willReturn(Optional.empty());
            given(starRecordCountQueryPort.countCompletedAfter(USER_ID, null)).willReturn(7);

            // when
            ReportGaugeResponse response = service.getGauge(USER_ID);

            // then
            assertThat(response.currentCount()).isEqualTo(7);
            assertThat(response.nextThreshold()).isEqualTo(10);
            assertThat(response.progressRate()).isEqualTo(0.7);
            assertThat(response.isGeneratable()).isFalse();
        }

        @Test
        @DisplayName("리포트 있음: 마지막 리포트 이후 완료 심화기록 수를 currentCount로 반환한다")
        void should_returnCountAfterLastReport_when_reportExists() {
            // given
            OffsetDateTime lastReportAt = OffsetDateTime.now().minusDays(5);
            Report lastReport =
                    report(REPORT_ID, USER_ID, ReportType.MINI, ReportStatus.SUCCESS, lastReportAt);
            given(reportQueryPort.findLatestSuccessByUserId(USER_ID))
                    .willReturn(Optional.of(lastReport));
            given(starRecordCountQueryPort.countCompletedAfter(USER_ID, lastReportAt))
                    .willReturn(4);

            // when
            ReportGaugeResponse response = service.getGauge(USER_ID);

            // then
            assertThat(response.currentCount()).isEqualTo(4);
            assertThat(response.nextThreshold()).isEqualTo(10);
            assertThat(response.isGeneratable()).isFalse();
        }

        @Test
        @DisplayName("currentCount >= 10이면 isGeneratable=true를 반환한다")
        void should_returnGeneratable_when_currentCountReachesThreshold() {
            // given
            OffsetDateTime lastReportAt = OffsetDateTime.now().minusDays(10);
            Report lastReport =
                    report(REPORT_ID, USER_ID, ReportType.MINI, ReportStatus.SUCCESS, lastReportAt);
            given(reportQueryPort.findLatestSuccessByUserId(USER_ID))
                    .willReturn(Optional.of(lastReport));
            given(starRecordCountQueryPort.countCompletedAfter(USER_ID, lastReportAt))
                    .willReturn(10);

            // when
            ReportGaugeResponse response = service.getGauge(USER_ID);

            // then
            assertThat(response.isGeneratable()).isTrue();
            assertThat(response.progressRate()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("currentCount > 10이면 progressRate가 1.0을 초과한다")
        void should_returnProgressRateOverOne_when_currentCountExceedsThreshold() {
            // given
            given(reportQueryPort.findLatestSuccessByUserId(USER_ID)).willReturn(Optional.empty());
            given(starRecordCountQueryPort.countCompletedAfter(USER_ID, null)).willReturn(12);

            // when
            ReportGaugeResponse response = service.getGauge(USER_ID);

            // then
            assertThat(response.currentCount()).isEqualTo(12);
            assertThat(response.progressRate()).isEqualTo(1.2);
            assertThat(response.isGeneratable()).isTrue();
        }
    }

    // ============================================================
    // getList
    // ============================================================
    @Nested
    @DisplayName("getList")
    class GetList {

        @Test
        @DisplayName("리포트 목록을 반환한다")
        void should_returnReportList() {
            // given
            Report mini =
                    report(
                            1L,
                            USER_ID,
                            ReportType.MINI,
                            ReportStatus.SUCCESS,
                            OffsetDateTime.now().minusDays(10));
            Report career =
                    report(
                            2L,
                            USER_ID,
                            ReportType.CAREER,
                            ReportStatus.SUCCESS,
                            OffsetDateTime.now());
            given(reportQueryPort.findAllByUserIdOrderByCreatedAtDesc(USER_ID))
                    .willReturn(List.of(career, mini));

            // when
            ReportListResponse response = service.getList(USER_ID);

            // then
            assertThat(response.reports()).hasSize(2);
            assertThat(response.reports().get(0).reportId()).isEqualTo(2L);
            assertThat(response.reports().get(1).reportId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("리포트 이력 없으면 빈 배열을 반환한다")
        void should_returnEmptyList_when_noReports() {
            // given
            given(reportQueryPort.findAllByUserIdOrderByCreatedAtDesc(USER_ID))
                    .willReturn(List.of());

            // when
            ReportListResponse response = service.getList(USER_ID);

            // then
            assertThat(response.reports()).isEmpty();
        }
    }

    // ============================================================
    // getDetail
    // ============================================================
    @Nested
    @DisplayName("getDetail")
    class GetDetail {

        @Test
        @DisplayName("정상 조회: SUCCESS 상태의 본인 리포트를 반환한다")
        void should_returnDetail_when_validRequest() {
            // given
            Report report =
                    report(
                            REPORT_ID,
                            USER_ID,
                            ReportType.CAREER,
                            ReportStatus.SUCCESS,
                            OffsetDateTime.now());
            given(reportQueryPort.findById(REPORT_ID)).willReturn(Optional.of(report));

            // when
            ReportDetailResponse response = service.getDetail(REPORT_ID, USER_ID);

            // then
            assertThat(response.reportId()).isEqualTo(REPORT_ID);
            assertThat(response.reportType()).isEqualTo(ReportType.CAREER);
        }

        @Test
        @DisplayName("리포트 없으면 REPORT_NOT_FOUND를 던진다")
        void should_throwReportNotFound_when_missing() {
            // given
            given(reportQueryPort.findById(REPORT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getDetail(REPORT_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_NOT_FOUND);
        }

        @Test
        @DisplayName("타 유저의 리포트면 FORBIDDEN을 던진다")
        void should_throwForbidden_when_otherUser() {
            // given
            Report report =
                    report(
                            REPORT_ID,
                            OTHER_USER_ID,
                            ReportType.CAREER,
                            ReportStatus.SUCCESS,
                            OffsetDateTime.now());
            given(reportQueryPort.findById(REPORT_ID)).willReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> service.getDetail(REPORT_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("SUCCESS 상태가 아닌 리포트면 REPORT_NOT_COMPLETED를 던진다")
        void should_throwReportNotCompleted_when_notSuccess() {
            // given
            Report report =
                    report(
                            REPORT_ID,
                            USER_ID,
                            ReportType.CAREER,
                            ReportStatus.GENERATING,
                            OffsetDateTime.now());
            given(reportQueryPort.findById(REPORT_ID)).willReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> service.getDetail(REPORT_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_NOT_COMPLETED);
        }
    }

    // ============== helpers ==============

    private static Report report(
            Long id,
            Long ownerUserId,
            ReportType type,
            ReportStatus status,
            OffsetDateTime createdAt) {
        Report report = new Report();
        ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "user", user(ownerUserId));
        ReflectionTestUtils.setField(report, "reportType", type);
        ReflectionTestUtils.setField(report, "status", status);
        ReflectionTestUtils.setField(report, "createdAt", createdAt);
        return report;
    }

    private static User user(Long id) {
        try {
            java.lang.reflect.Constructor<User> ctor = User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            User user = ctor.newInstance();
            ReflectionTestUtils.setField(user, "id", id);
            return user;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
