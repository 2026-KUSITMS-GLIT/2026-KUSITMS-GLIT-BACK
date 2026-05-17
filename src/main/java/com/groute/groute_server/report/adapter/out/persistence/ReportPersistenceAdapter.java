package com.groute.groute_server.report.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.groute.groute_server.report.application.port.out.LoadReportPort;
import com.groute.groute_server.report.application.port.out.ReportQueryPort;
import com.groute.groute_server.report.application.port.out.SaveReportPort;
import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportStatus;
import com.groute.groute_server.report.domain.enums.ReportType;

import lombok.RequiredArgsConstructor;

/**
 * {@link ReportQueryPort}, {@link LoadReportPort}와 {@link SaveReportPort}의 JPA 어댑터.
 *
 * <p>리포트 목록·게이지 조회, 단건 조회, 미니 이력 확인, 최신 리포트 조회, 저장을 담당한다.
 */
@Component
@RequiredArgsConstructor
class ReportPersistenceAdapter implements ReportQueryPort, LoadReportPort, SaveReportPort {

    private final ReportJpaRepository jpaRepository;

    // ReportQueryPort

    @Override
    public List<Report> findAllByUserIdOrderByCreatedAtDesc(Long userId) {
        return jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Optional<Report> findLatestSuccessByUserId(Long userId) {
        return jpaRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                userId, ReportStatus.SUCCESS);
    }

    // LoadReportPort

    @Override
    public Optional<Report> findById(Long reportId) {
        return jpaRepository.findById(reportId);
    }

    @Override
    public Optional<Report> findLatestByUserId(Long userId) {
        return jpaRepository.findLatestByUserId(userId);
    }

    @Override
    public boolean existsMiniReportByUserId(Long userId) {
        return jpaRepository.existsByUserIdAndReportTypeAndStatus(
                userId, ReportType.MINI, ReportStatus.SUCCESS);
    }

    // SaveReportPort

    @Override
    public Report save(Report report) {
        return jpaRepository.save(report);
    }
}
