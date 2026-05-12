package com.groute.groute_server.report.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.report.domain.Report;
import com.groute.groute_server.report.domain.enums.ReportStatus;
import com.groute.groute_server.report.domain.enums.ReportType;

/**
 * 리포트 JPA 레포지토리.
 *
 * <p>목록·상세·게이지 조회 및 리포트 생성 상태 관리에 사용한다.
 */
public interface ReportJpaRepository extends JpaRepository<Report, Long> {

    /** 유저의 리포트 목록을 생성일 기준 내림차순으로 조회한다. */
    List<Report> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /** 유저의 가장 최근 성공한 리포트를 조회한다. 게이지 계산 기준점으로 사용한다. */
    Optional<Report> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ReportStatus status);

    /** 유저의 미니 리포트 발행 이력이 있는지 확인한다. */
    boolean existsByUserIdAndReportType(Long userId, ReportType reportType);

    /** 유저의 가장 최근 리포트를 조회한다. */
    @Query(
            "SELECT r FROM Report r "
                    + "WHERE r.user.id = :userId "
                    + "ORDER BY r.createdAt DESC "
                    + "LIMIT 1")
    Optional<Report> findLatestByUserId(@Param("userId") Long userId);
}
