package com.groute.groute_server.report.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.groute.groute_server.report.application.port.out.ReportQueryPort;
import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportStatus;

import lombok.RequiredArgsConstructor;

/**
 * {@link ReportQueryPort}의 JPA 어댑터.
 *
 * <p>리포트 목록·상세·게이지 계산용 조회를 담당한다(RPT-001).
 */
@Component
@RequiredArgsConstructor
class ReportPersistenceAdapter implements ReportQueryPort {

    private final ReportJpaRepository jpaRepository;

    @Override
    public List<Report> findAllByUserIdOrderByCreatedAtDesc(Long userId) {
        return jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Optional<Report> findById(Long reportId) {
        return jpaRepository.findById(reportId);
    }

    @Override
    public Optional<Report> findLatestSuccessByUserId(Long userId) {
        return jpaRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                userId, ReportStatus.SUCCESS);
    }
}
