package com.groute.groute_server.report.application.port.in;

import java.util.List;

/**
 * 리포트 생성용 사전 정보 조회(RPT-002) 응답 모델.
 *
 * @param reportType 서버가 결정한 리포트 타입 (MINI / CAREER)
 * @param totalStarCount 유저 전체 완료된 심화기록 수 (달력 하단 카운터 분모)
 * @param autoSelectedStarRecordIds 자동 선택된 심화기록 ID 목록 (최신순)
 * @param starRecordDates 완료된 심화기록이 있는 날짜 목록 (달력 하이라이트용, 전체 기간)
 */
public record SelectableInfoView(
        String reportType,
        int totalStarCount,
        List<Long> autoSelectedStarRecordIds,
        List<String> starRecordDates) {}
