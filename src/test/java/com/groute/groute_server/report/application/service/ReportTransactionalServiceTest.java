package com.groute.groute_server.report.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
import com.groute.groute_server.report.application.port.out.LoadReportPort;
import com.groute.groute_server.report.application.port.out.LoadStarRecordPort;
import com.groute.groute_server.report.application.port.out.LoadUserPort;
import com.groute.groute_server.report.application.port.out.SaveReportPort;
import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportStatus;
import com.groute.groute_server.report.domain.enums.ReportType;
import com.groute.groute_server.user.entity.User;

@ExtendWith(MockitoExtension.class)
class ReportTransactionalServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long REPORT_ID = 10L;

    @Mock LoadReportPort loadReportPort;
    @Mock SaveReportPort saveReportPort;
    @Mock LoadStarRecordPort loadStarRecordPort;
    @Mock LoadUserPort loadUserPort;

    @InjectMocks ReportTransactionalService service;

    // =========================================================
    // saveReportTx
    // =========================================================

    @Nested
    @DisplayName("리포트 생성 DB 저장")
    class SaveReportTx {

        @Test
        @DisplayName("MINI 정상 생성 시 CreateReportResult를 반환한다")
        void should_returnResult_when_miniCreatedSuccessfully() {
            // given
            List<Long> ids = ids(10);
            given(loadReportPort.existsMiniReportByUserId(USER_ID)).willReturn(false);
            given(loadUserPort.findUserById(USER_ID)).willReturn(user(USER_ID));
            given(loadStarRecordPort.countCompletedByUserId(USER_ID)).willReturn(10);
            given(loadStarRecordPort.findAllByIds(USER_ID, ids)).willReturn(starRecords(10));
            given(loadStarRecordPort.findScrumsByStarRecordIds(USER_ID, ids)).willReturn(List.of());
            Report saved = report(REPORT_ID, USER_ID, ReportType.MINI, ReportStatus.GENERATING, 0);
            given(saveReportPort.save(any())).willReturn(saved);

            // when
            ReportTransactionalService.CreateReportResult result =
                    service.saveReportTx(new CreateReportCommand(USER_ID, ReportType.MINI, ids));

            // then
            assertThat(result.reportId()).isEqualTo(REPORT_ID);
            assertThat(result.starRecords()).hasSize(10);
        }

        @Test
        @DisplayName("MINI 중복 요청 시 REPORT_MINI_ALREADY_EXISTS를 던진다")
        void should_throwMiniAlreadyExists_when_miniAlreadyCreated() {
            // given
            given(loadReportPort.existsMiniReportByUserId(USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(
                            () ->
                                    service.saveReportTx(
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
                                    service.saveReportTx(
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
                                    service.saveReportTx(
                                            new CreateReportCommand(
                                                    USER_ID, ReportType.CAREER, ids(19))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_INVALID_STAR_COUNT);
        }

        @Test
        @DisplayName("findAllByIds 결과가 요청 개수보다 적으면 REPORT_INVALID_STAR_COUNT를 던진다")
        void should_throwInvalidStarCount_when_loadedCountMismatch() {
            // given — 10개 요청했는데 8개만 로드됨 (2개는 타인 소유 또는 존재하지 않음)
            List<Long> ids = ids(10);
            given(loadReportPort.existsMiniReportByUserId(USER_ID)).willReturn(false);
            given(loadUserPort.findUserById(USER_ID)).willReturn(user(USER_ID));
            given(loadStarRecordPort.countCompletedByUserId(USER_ID)).willReturn(10);
            given(saveReportPort.save(any()))
                    .willReturn(
                            report(
                                    REPORT_ID,
                                    USER_ID,
                                    ReportType.MINI,
                                    ReportStatus.GENERATING,
                                    0));
            given(loadStarRecordPort.findAllByIds(USER_ID, ids)).willReturn(starRecords(8));

            // when & then
            assertThatThrownBy(
                            () ->
                                    service.saveReportTx(
                                            new CreateReportCommand(USER_ID, ReportType.MINI, ids)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_INVALID_STAR_COUNT);
        }
    }

    // =========================================================
    // retryReportTx
    // =========================================================

    @Nested
    @DisplayName("리포트 재시도 DB 상태 변경")
    class RetryReportTx {

        @Test
        @DisplayName("FAILED 상태이고 재시도 가능하면 정상 처리된다")
        void should_succeed_when_retryAvailable() {
            // given
            Report report = report(REPORT_ID, USER_ID, ReportType.MINI, ReportStatus.FAILED, 0);
            given(loadReportPort.findById(REPORT_ID)).willReturn(Optional.of(report));
            given(saveReportPort.save(any())).willReturn(report);

            // when & then — 예외 없이 정상 완료
            service.retryReportTx(USER_ID, REPORT_ID);
        }

        @Test
        @DisplayName("재시도 불가 상태이면 REPORT_RETRY_NOT_AVAILABLE을 던진다")
        void should_throwRetryNotAvailable_when_alreadyRetried() {
            // given — retryCount=1이면 재시도 불가
            Report report = report(REPORT_ID, USER_ID, ReportType.MINI, ReportStatus.FAILED, 1);
            given(loadReportPort.findById(REPORT_ID)).willReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> service.retryReportTx(USER_ID, REPORT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_RETRY_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("타인의 리포트면 FORBIDDEN을 던진다")
        void should_throwForbidden_when_otherUserReport() {
            // given
            Report report =
                    report(REPORT_ID, OTHER_USER_ID, ReportType.MINI, ReportStatus.FAILED, 0);
            given(loadReportPort.findById(REPORT_ID)).willReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> service.retryReportTx(USER_ID, REPORT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 리포트면 REPORT_NOT_FOUND를 던진다")
        void should_throwReportNotFound_when_missing() {
            // given
            given(loadReportPort.findById(REPORT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.retryReportTx(USER_ID, REPORT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REPORT_NOT_FOUND);
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
