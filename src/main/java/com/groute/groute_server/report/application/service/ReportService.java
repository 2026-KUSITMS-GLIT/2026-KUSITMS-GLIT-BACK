package com.groute.groute_server.report.application.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.domain.Scrum;
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
import com.groute.groute_server.report.application.port.out.SaveReportPort;
import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportType;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 리포트 생성 플로우 서비스.
 *
 * <p>미니/커리어 타입을 판단하여 사전 정보를 제공하고, 유저가 선택한 심화기록을 바탕으로 AI 서버에 리포트 생성을 요청한다. AI 생성은 비동기로 진행되며 프론트는 상태
 * 폴링으로 완료 여부를 확인한다. 생성 실패 시 1회에 한해 재시도를 제공한다.
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
    private final SaveReportPort saveReportPort;
    private final LoadStarRecordPort loadStarRecordPort;
    private final RequestAiReportPort requestAiReportPort;
    private final UserRepository userRepository;

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
     * <p>MINI 중복 요청 및 심화기록 개수 검증 후, 선택된 심화기록 날짜의 스크럼을 자동 수집하여 AI 인풋을 구성한다.
     */
    @Override
    @Transactional
    public Long createReport(CreateReportCommand command) {
        // 1. MINI 요청인데 미니 이력이 이미 있으면 400
        if (command.reportType() == ReportType.MINI
                && loadReportPort.existsMiniReportByUserId(command.userId())) {
            throw new BusinessException(ErrorCode.REPORT_MINI_ALREADY_EXISTS);
        }

        // 2. starRecordIds 개수 검증
        validateStarRecordCount(command.reportType(), command.starRecordIds().size());

        // 3. 유저 조회
        User user =
                userRepository
                        .findById(command.userId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 전체 완료 심화기록 수 (star_count_at 기록용)
        int totalStarCount = loadStarRecordPort.countCompletedByUserId(command.userId());

        // 5. reports row INSERT
        Report report = Report.create(user, command.reportType(), totalStarCount);
        Report savedReport = saveReportPort.save(report);

        // 6. 선택된 심화기록 로드 (userId로 소유권 검증)
        List<StarRecord> starRecords =
                loadStarRecordPort.findAllByIds(command.userId(), command.starRecordIds());

        // 7. 선택된 심화기록 날짜의 스크럼 자동 수집
        List<Scrum> scrums =
                loadStarRecordPort.findScrumsByStarRecordIds(
                        command.userId(), command.starRecordIds());

        // 8. AI 서버 비동기 호출 (현재 stub)
        requestAiReportPort.requestReportGeneration(savedReport.getId(), starRecords, scrums);

        return savedReport.getId();
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
     * <p>재시도는 1회만 허용되며, 조건을 만족하지 않으면 {@link
     * com.groute.groute_server.common.exception.BusinessException}을 던진다.
     */
    @Override
    @Transactional
    public Long retryReport(Long userId, Long reportId) {
        Report report =
                loadReportPort
                        .findById(reportId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        validateOwnership(report, userId);

        // startRetry() 내부에서 isRetryAvailable() 검증 후 예외 처리
        report.startRetry();
        saveReportPort.save(report);

        // AI 재호출 (stub) — 원본 starRecords/scrums 재조회 없이 reportId만 넘김
        requestAiReportPort.requestReportGeneration(report.getId(), List.of(), List.of());

        return report.getId();
    }

    // =========================================================
    // private
    // =========================================================

    private void validateStarRecordCount(ReportType reportType, int size) {
        if (reportType == ReportType.MINI && size != MINI_LIMIT) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_STAR_COUNT);
        }
        if (reportType == ReportType.CAREER && size < CAREER_LIMIT) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_STAR_COUNT);
        }
    }

    private void validateOwnership(Report report, Long userId) {
        if (!Objects.equals(report.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
