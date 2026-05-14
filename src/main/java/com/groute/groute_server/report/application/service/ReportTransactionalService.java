package com.groute.groute_server.report.application.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.report.application.port.in.CreateReportCommand;
import com.groute.groute_server.report.application.port.out.LoadReportPort;
import com.groute.groute_server.report.application.port.out.LoadStarRecordPort;
import com.groute.groute_server.report.application.port.out.SaveReportPort;
import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportType;
import com.groute.groute_server.user.entity.User;
import com.groute.groute_server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 리포트 DB 작업 전담 서비스.
 *
 * <p>AI 호출과 트랜잭션을 분리하기 위해 {@link ReportService}에서 분리된 클래스. 트랜잭션 커밋 후 AI 호출이 실행되도록, DB 저장 로직만 여기에
 * 위치한다.
 */
@Service
@RequiredArgsConstructor
public class ReportTransactionalService {

    private static final int MINI_LIMIT = 10;
    private static final int CAREER_LIMIT = 20;

    private final LoadReportPort loadReportPort;
    private final SaveReportPort saveReportPort;
    private final LoadStarRecordPort loadStarRecordPort;
    private final UserRepository userRepository;

    /**
     * 리포트 생성 요청의 DB 작업을 수행한다.
     *
     * <p>검증 후 reports row를 INSERT하고, AI 호출에 필요한 데이터를 반환한다. 메서드 리턴 시점에 트랜잭션이 커밋된다.
     */
    @Transactional
    public CreateReportResult saveReportTx(CreateReportCommand command) {
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

        // 6-1. 실제 로드된 개수 검증 (존재하지 않거나 타인 소유 ID 혼입 방지)
        if (starRecords.size() != command.starRecordIds().size()) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_STAR_COUNT);
        }

        // 7. 선택된 심화기록 날짜의 스크럼 자동 수집
        List<Scrum> scrums =
                loadStarRecordPort.findScrumsByStarRecordIds(
                        command.userId(), command.starRecordIds());

        return new CreateReportResult(savedReport.getId(), starRecords, scrums);
    }

    /**
     * 리포트 재시도의 DB 상태 변경을 수행한다.
     *
     * <p>소유권 및 재시도 가능 여부 검증 후 GENERATING으로 상태를 전환한다. 메서드 리턴 시점에 트랜잭션이 커밋된다.
     */
    @Transactional
    public void retryReportTx(Long userId, Long reportId) {
        Report report =
                loadReportPort
                        .findById(reportId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        if (!Objects.equals(report.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // startRetry() 내부에서 isRetryAvailable() 검증 후 예외 처리
        report.startRetry();
        saveReportPort.save(report);
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

    /** AI 호출에 필요한 데이터를 담는 레코드. */
    public record CreateReportResult(
            Long reportId, List<StarRecord> starRecords, List<Scrum> scrums) {}
}
