package com.groute.groute_server.report.application.port.out;

import java.util.List;
import java.util.Optional;

import com.groute.groute_server.report.domain.Report;

/**
 * 리포트 조회 포트.
 *
 * <p>reports 테이블 조회 전용. 목록·상세·마지막 리포트 조회를 담당한다(RPT-001).
 */
public interface ReportQueryPort {

    /** 유저의 리포트 목록을 생성일 기준 내림차순으로 조회한다. */
    List<Report> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /** 리포트 단건 조회. */
    Optional<Report> findById(Long reportId);

    /** 유저의 가장 최근 성공한 리포트를 조회한다. 게이지 계산 기준점으로 사용한다. */
    Optional<Report> findLatestSuccessByUserId(Long userId);
}
