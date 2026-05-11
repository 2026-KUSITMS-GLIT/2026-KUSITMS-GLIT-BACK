package com.groute.groute_server.report.application.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groute.groute_server.common.exception.BusinessException;
import com.groute.groute_server.common.exception.ErrorCode;
import com.groute.groute_server.report.adapter.in.web.dto.ReportDetailResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportGaugeResponse;
import com.groute.groute_server.report.adapter.in.web.dto.ReportListResponse;
import com.groute.groute_server.report.application.port.in.GetReportDetailUseCase;
import com.groute.groute_server.report.application.port.in.GetReportGaugeUseCase;
import com.groute.groute_server.report.application.port.in.GetReportListUseCase;
import com.groute.groute_server.report.application.port.out.ReportQueryPort;
import com.groute.groute_server.report.application.port.out.StarRecordCountQueryPort;
import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportStatus;

import lombok.RequiredArgsConstructor;

/**
 * 리포트 조회 서비스 (RPT-001).
 *
 * <p>게이지·목록·상세 조회 유스케이스를 구현한다. 외부 의존성은 포트 인터페이스를 통해서만 접근한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportQueryService
        implements GetReportGaugeUseCase, GetReportListUseCase, GetReportDetailUseCase {

    private static final int NEXT_THRESHOLD = 10;

    private final ReportQueryPort reportQueryPort;
    private final StarRecordCountQueryPort starRecordCountQueryPort;

    /**
     * RPT-001: 리포트 게이지 조회.
     *
     * <p>마지막 성공한 리포트 생성 시점 이후 완료된 심화기록 수를 반환한다. 분모는 항상 10 고정. 리포트가 없는 신규 유저는 전체 완료된 심화기록 수를 기준으로
     * 한다.
     */
    @Override
    public ReportGaugeResponse getGauge(Long userId) {
        // 1. 마지막 성공한 리포트 조회 → 기준 시점 추출
        OffsetDateTime after =
                reportQueryPort
                        .findLatestSuccessByUserId(userId)
                        .map(Report::getCreatedAt)
                        .orElse(null);

        // 2. 기준 시점 이후 완료된 심화기록 수 조회 (10 초과 시에도 계속 누적)
        int currentCount = starRecordCountQueryPort.countCompletedAfter(userId, after);

        return ReportGaugeResponse.of(currentCount, NEXT_THRESHOLD);
    }

    /**
     * RPT-001: 리포트 목록 조회.
     *
     * <p>생성일 기준 내림차순 정렬. 이력 없으면 빈 배열 반환.
     */
    @Override
    public ReportListResponse getList(Long userId) {
        List<Report> reports = reportQueryPort.findAllByUserIdOrderByCreatedAtDesc(userId);
        return ReportListResponse.from(reports);
    }

    /**
     * RPT-001: 리포트 상세 조회.
     *
     * <p>소유자 검증 후 MINI/CAREER 타입별 content를 반환한다.
     */
    @Override
    public ReportDetailResponse getDetail(Long reportId, Long userId) {
        // 1. 리포트 조회
        Report report =
                reportQueryPort
                        .findById(reportId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        // 2. 소유자 검증
        if (!report.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. 생성 완료된 리포트만 조회 가능
        if (report.getStatus() != ReportStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.REPORT_NOT_COMPLETED);
        }

        return ReportDetailResponse.from(report);
    }
}
