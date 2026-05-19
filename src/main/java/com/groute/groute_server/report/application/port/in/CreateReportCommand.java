package com.groute.groute_server.report.application.port.in;

import java.util.List;

import com.groute.groute_server.report.domain.enums.ReportType;

/**
 * 리포트 생성 요청 입력.
 *
 * @param userId 요청 유저 PK
 * @param reportType 클라이언트가 전달한 리포트 타입
 * @param starRecordIds 유저가 선택한 심화기록 ID 목록
 */
public record CreateReportCommand(Long userId, ReportType reportType, List<Long> starRecordIds) {}
