package com.groute.groute_server.report.application.port.out;

import java.util.Optional;

import com.groute.groute_server.report.domain.Report;

/** 리포트 조회 포트. 리포트 존재 여부 및 최신 리포트 조회를 담당한다. */
public interface LoadReportPort {

    /**
     * 리포트 ID로 조회한다.
     *
     * @param reportId 리포트 PK
     * @return 리포트 Optional
     */
    Optional<Report> findById(Long reportId);

    /**
     * 유저의 가장 최근 리포트를 조회한다.
     *
     * <p>CAREER 타입 판단 시 마지막 리포트 생성 시각 기준이 필요할 때 사용한다.
     *
     * @param userId 유저 PK
     * @return 가장 최근 리포트 Optional
     */
    Optional<Report> findLatestByUserId(Long userId);

    /**
     * 유저의 미니 리포트 발행 이력이 있는지 확인한다.
     *
     * @param userId 유저 PK
     * @return 미니 리포트 존재 여부
     */
    boolean existsMiniReportByUserId(Long userId);

}
