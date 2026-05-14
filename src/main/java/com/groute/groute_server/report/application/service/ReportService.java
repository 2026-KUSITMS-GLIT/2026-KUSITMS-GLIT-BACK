package com.groute.groute_server.report.application.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.report.application.port.in.CreateReportCommand;
import com.groute.groute_server.report.application.port.in.CreateReportUseCase;
import com.groute.groute_server.report.application.port.in.GetReportStatusUseCase;
import com.groute.groute_server.report.application.port.in.GetSelectableInfoUseCase;
import com.groute.groute_server.report.application.port.in.ReportStatusView;
import com.groute.groute_server.report.application.port.in.RetryReportUseCase;
import com.groute.groute_server.report.application.port.in.SelectableInfoView;
import com.groute.groute_server.report.application.port.out.LoadReportPort;
import com.groute.groute_server.report.application.port.out.LoadStarRecordPort;
import com.groute.groute_server.report.application.port.out.RequestAiReportPort;
import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportType;

import lombok.RequiredArgsConstructor;

/**
 * 리포트 생성 플로우 서비스.
 *
 * <p>미니/커리어 타입을 판단하여 사전 정보를 제공하고, 유저가 선택한 심화기록을 바탕으로 AI 서버에 리포트 생성을 요청한다. AI 생성은 비동기로 진행되며 프론트는 상태
 * 폴링으로 완료 여부를 확인한다. 생성 실패 시 1회에 한해 재시도를 제공한다.
 *
 * <p>DB 작업은 {@link ReportTransactionalService}에 위임하여 트랜잭션 커밋 후 AI 호출이 실행되도록 분리한다.
 */
@Service
@RequiredArgsConstructor
public class ReportService
        implements GetSelectableInfoUseCase,
                CreateReportUseCase,
                GetReportStatusUseCase,
                RetryReportUseCase {

    private static final int MINI_LIMIT = 10;
    private static final int CAREER_LIMIT = 20;

    private final LoadReportPort loadReportPort;
    private final LoadStarRecordPort loadStarRecordPort;
    private final RequestAiReportPort requestAiReportPort;
    private final ReportTransactionalService reportTransactionalService;

    // =========================================================
    // 사전 정보 조회
    // =========================================================

    /**
     * 미니/커리어 타입을 판단하고 달력 렌더링에 필요한 심화기록 날짜 목록과 달력 화면 진입 시 자동으로 체크될 심화기록 ID 목록을 반환한다.
     *
     * <p>미니 발행 이력이 없으면 MINI, 있으면 CAREER 타입으로 결정한다.
     */
    @Override
    @Transactional(readOnly = true)
    public SelectableInfoView getSelectableInfo(Long userId) {
        boolean hasMini = loadReportPort.existsMiniReportByUserId(userId);
        ReportType reportType = hasMini ? ReportType.CAREER : ReportType.MINI;
        int limit = hasMini ? CAREER_LIMIT : MINI_LIMIT;

        int totalStarCount = loadStarRecordPort.countCompletedByUserId(userId);

        List<Long> autoSelectedIds =
                loadStarRecordPort.findCompletedByUserIdOrderByLatest(userId, limit).stream()
                        .map(StarRecord::getId)
                        .toList();

        List<String> starRecordDates =
                loadStarRecordPort.findCompletedStarDatesByUserId(userId).stream()
                        .map(date -> date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .toList();

        return new SelectableInfoView(
                reportType.name(), totalStarCount, autoSelectedIds, starRecordDates);
    }

    // =========================================================
    // 리포트 생성 요청
    // =========================================================

    /**
     * 유저가 선택한 심화기록을 검증하고 리포트 row를 생성한 뒤 AI 서버에 생성을 요청한다.
     *
     * <p>DB 저장은 {@link ReportTransactionalService#saveReportTx}에 위임하여 트랜잭션 커밋 후 AI 호출이 실행된다.
     */
    @Override
    public Long createReport(CreateReportCommand command) {
        // 1~7. 검증 + DB 저장 (트랜잭션 커밋까지 완료)
        ReportTransactionalService.CreateReportResult result =
                reportTransactionalService.saveReportTx(command);

        // 8. AI 서버 비동기 호출 (현재 stub) — 트랜잭션 밖에서 실행
        requestAiReportPort.requestReportGeneration(
                result.reportId(), result.starRecords(), result.scrums());

        return result.reportId();
    }

    // =========================================================
    // 생성 상태 폴링
    // =========================================================

    /**
     * 리포트 생성 상태를 반환한다.
     *
     * <p>FAILED 상태이고 재시도가 가능한 경우 retryAvailable=true를 함께 반환한다.
     */
    @Override
    @Transactional(readOnly = true)
    public ReportStatusView getReportStatus(Long userId, Long reportId) {
        Report report =
                loadReportPort
                        .findById(reportId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        validateOwnership(report, userId);

        Boolean retryAvailable = report.isRetryAvailable() ? true : null;

        return new ReportStatusView(report.getId(), report.getStatus().name(), retryAvailable);
    }

    // =========================================================
    // 생성 재시도
    // =========================================================

    /**
     * FAILED 상태인 리포트를 GENERATING으로 되돌리고 AI 서버에 재호출한다.
     *
     * <p>DB 상태 변경은 {@link ReportTransactionalService#retryReportTx}에 위임하여 트랜잭션 커밋 후 AI 호출이 실행된다.
     */
    @Override
    public Long retryReport(Long userId, Long reportId) {
        // DB 상태 변경 (트랜잭션 커밋까지 완료)
        reportTransactionalService.retryReportTx(userId, reportId);

        // AI 재호출 (stub) — 트랜잭션 밖에서 실행
        requestAiReportPort.requestReportGeneration(reportId, List.of(), List.of());

        return reportId;
    }

    // =========================================================
    // private
    // =========================================================

    private void validateOwnership(Report report, Long userId) {
        if (!Objects.equals(report.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
