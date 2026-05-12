package com.groute.groute_server.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.report.application.port.in.CreateReportCommand;
import com.groute.groute_server.report.application.port.in.ReportStatusView;
import com.groute.groute_server.report.application.port.in.SelectableInfoView;
import com.groute.groute_server.report.application.port.out.LoadReportPort;
import com.groute.groute_server.report.application.port.out.LoadStarRecordPort;
import com.groute.groute_server.report.application.port.out.RequestAiReportPort;
import com.groute.groute_server.report.application.port.out.SaveReportPort;
import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportStatus;
import com.groute.groute_server.report.domain.enums.ReportType;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long REPORT_ID = 10L;

    @Mock LoadReportPort loadReportPort;
    @Mock SaveReportPort saveReportPort;
    @Mock LoadStarRecordPort loadStarRecordPort;
    @Mock RequestAiReportPort requestAiReportPort;
    @Mock UserRepository userRepository;

    @InjectMocks ReportService service;

    // =========================================================
    // getSelectableInfo
    // =========================================================

    @Nested
    @DisplayName("사전 정보 조회")
    class GetSelectableInfo {

        @Test
        @DisplayName("미니 이력 없으면 MINI 타입과 최신 10개 ID를 반환한다")
        void should_returnMiniType_when_noMiniHistory() {
            // given
            given(loadReportPort.existsMiniReportByUserId(USER_ID)).willReturn(false);
            given(loadStarRecordPort.countCompletedByUserId(USER_ID)).willReturn(15);
            given(loadStarRecordPort.findCompletedByUserIdOrderByLatest(USER_ID, 10))
                    .willReturn(starRecords(10));
            given(loadStarRecordPort.findCompletedStarDatesByUserId(USER_ID))
                    .willReturn(List.of("2026-04-09", "2026-04-07"));

            // when
            SelectableInfoView view = service.getSelectableInfo(USER_ID);

            // then
            assertThat(view.reportType()).isEqualTo("MINI");
            assertThat(view.totalStarCount()).isEqualTo(15);
            assertThat(view.autoSelectedStarRecordIds()).hasSize(10);
            assertThat(view.starRecordDates()).containsExactly("2026-04-09", "2026-04-07");
        }

        @Test
        @DisplayName("미니 이력 있으면 CAREER 타입과 최신 20개 ID를 반환한다")
        void should_returnCareerType_when_hasMiniHistory() {
            // given
            given(loadReportPort.existsMiniReportByUserId(USER_ID)).willReturn(true);
            given(loadStarRecordPort.countCompletedByUserId(USER_ID)).willReturn(25);
            given(loadStarRecordPort.findCompletedByUserIdOrderByLatest(USER_ID, 20))
                    .willReturn(starRecords(20));
            given(loadStarRecordPort.findCompletedStarDatesByUserId(USER_ID))
                    .willReturn(List.of("2026-04-09"));

            // when
            SelectableInfoView view = service.getSelectableInfo(USER_ID);

            // then
            assertThat(view.reportType()).isEqualTo("CAREER");
            assertThat(view.totalStarCount()).isEqualTo(25);
            assertThat(view.autoSelectedStarRecordIds()).hasSize(20);
        }
    }

    // =========================================================
    // createReport
    // =========================================================

    @Nested
    @DisplayName("리포트 생성 요청")
    class CreateReport {

        @Test
        @DisplayName("MINI 정상 생성 시 reportId를 반환한다")
        void should_returnReportId_when_miniCreatedSuccessfully() {
            // given
            List<Long> ids = ids(10);
            given(loadReportPort.existsMiniReportByUserId(USER_ID)).willReturn(false);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(loadStarRecordPort.countCompletedByUserId(USER_ID)).willReturn(10);
            given(loadStarRecordPort.findAllByIds(USER_ID, ids)).willReturn(starRecords(10));
            given(loadStarRecordPort.findScrumsByStarRecordIds(USER_ID, ids)).willReturn(List.of());
            Report saved = report(REPORT_ID, USER_ID, ReportType.MINI, ReportStatus.GENERATING, 0);
            given(saveReportPort.save(any())).willReturn(saved);

            // when
            Long reportId =
                    service.createReport(new CreateReportCommand(USER_ID, ReportType.MINI, ids));

            // then
            assertThat(reportId).isEqualTo(REPORT_ID);
            then(requestAiReportPort)
                    .should()
                    .requestReportGeneration(anyLong(), anyList(), anyList());
        }

        @Test
        @DisplayName("MINI 중복 요청 시 REPORT_MINI_ALREADY_EXISTS를 던진다")
        void should_throwMiniAlreadyExists_when_miniAlreadyCreated() {
            // given
            given(loadReportPort.existsMiniReportByUserId(USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(
                            () ->
                                    service.createReport(
                                            new CreateReportCommand(
                                                    USER_ID, ReportType.MINI, ids(10))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_MINI_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("MINI starRecordIds가 10개가 아니면 REPORT_INVALID_STAR_COUNT를 던진다")
        void should_throwInvalidStarCount_when_miniIdsNotTen() {
            // given
            given(loadReportPort.existsMiniReportByUserId(USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(
                            () ->
                                    service.createReport(
                                            new CreateReportCommand(
                                                    USER_ID, ReportType.MINI, ids(9))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_INVALID_STAR_COUNT);
        }

        @Test
        @DisplayName("CAREER starRecordIds가 20개 미만이면 REPORT_INVALID_STAR_COUNT를 던진다")
        void should_throwInvalidStarCount_when_careerIdsLessThanTwenty() {
            // when & then
            assertThatThrownBy(
                            () ->
                                    service.createReport(
                                            new CreateReportCommand(
                                                    USER_ID, ReportType.CAREER, ids(19))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_INVALID_STAR_COUNT);
        }
    }

    // =========================================================
    // getReportStatus
    // =========================================================

    @Nested
    @DisplayName("생성 상태 폴링")
    class GetReportStatus {

        @Test
        @DisplayName("GENERATING 상태를 반환한다")
        void should_returnGenerating_when_stillProcessing() {
            // given
            Report report = report(REPORT_ID, USER_ID, ReportType.MINI, ReportStatus.GENERATING, 0);
            given(loadReportPort.findById(REPORT_ID)).willReturn(Optional.of(report));

            // when
            ReportStatusView view = service.getReportStatus(USER_ID, REPORT_ID);

            // then
            assertThat(view.status()).isEqualTo("GENERATING");
            assertThat(view.retryAvailable()).isNull();
        }

        @Test
        @DisplayName("FAILED 상태이고 재시도 가능하면 retryAvailable=true를 반환한다")
        void should_returnRetryAvailableTrue_when_failedAndRetryable() {
            // given
            Report report = report(REPORT_ID, USER_ID, ReportType.MINI, ReportStatus.FAILED, 0);
            given(loadReportPort.findById(REPORT_ID)).willReturn(Optional.of(report));

            // when
            ReportStatusView view = service.getReportStatus(USER_ID, REPORT_ID);

            // then
            assertThat(view.status()).isEqualTo("FAILED");
            assertThat(view.retryAvailable()).isTrue();
        }

        @Test
        @DisplayName("타 유저의 리포트 조회 시 FORBIDDEN을 던진다")
        void should_throwForbidden_when_otherUserReport() {
            // given
            Report report =
                    report(REPORT_ID, OTHER_USER_ID, ReportType.MINI, ReportStatus.GENERATING, 0);
            given(loadReportPort.findById(REPORT_ID)).willReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> service.getReportStatus(USER_ID, REPORT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 리포트 조회 시 REPORT_NOT_FOUND를 던진다")
        void should_throwReportNotFound_when_missing() {
            // given
            given(loadReportPort.findById(REPORT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getReportStatus(USER_ID, REPORT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_NOT_FOUND);
        }
    }

    // =========================================================
    // retryReport
    // =========================================================

    @Nested
    @DisplayName("생성 재시도")
    class RetryReport {

        @Test
        @DisplayName("FAILED 상태이고 재시도 가능하면 GENERATING으로 전환하고 reportId를 반환한다")
        void should_returnReportId_when_retrySuccessfully() {
            // given
            Report report = report(REPORT_ID, USER_ID, ReportType.MINI, ReportStatus.FAILED, 0);
            given(loadReportPort.findById(REPORT_ID)).willReturn(Optional.of(report));
            given(saveReportPort.save(any())).willReturn(report);

            // when
            Long reportId = service.retryReport(USER_ID, REPORT_ID);

            // then
            assertThat(reportId).isEqualTo(REPORT_ID);
            then(requestAiReportPort)
                    .should()
                    .requestReportGeneration(anyLong(), anyList(), anyList());
        }

        @Test
        @DisplayName("재시도 불가 상태이면 REPORT_RETRY_NOT_AVAILABLE을 던진다")
        void should_throwRetryNotAvailable_when_alreadyRetried() {
            // given — retryCount=1 이면 재시도 불가
            Report report = report(REPORT_ID, USER_ID, ReportType.MINI, ReportStatus.FAILED, 1);
            given(loadReportPort.findById(REPORT_ID)).willReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> service.retryReport(USER_ID, REPORT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_RETRY_NOT_AVAILABLE);
        }
    }

    // =========================================================
    // helpers
    // =========================================================

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

    private static Report report(
            Long id, Long userId, ReportType type, ReportStatus status, int retryCount) {
        Report report = new Report();
        ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "user", user(userId));
        ReflectionTestUtils.setField(report, "reportType", type);
        ReflectionTestUtils.setField(report, "status", status);
        ReflectionTestUtils.setField(report, "retryCount", (short) retryCount);
        return report;
    }

    private static List<StarRecord> starRecords(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(
                        i -> {
                            StarRecord sr = new StarRecord();
                            ReflectionTestUtils.setField(sr, "id", (long) (i + 1));
                            return sr;
                        })
                .toList();
    }

    private static List<Long> ids(int count) {
        return java.util.stream.LongStream.rangeClosed(1, count).boxed().toList();
    }
}
