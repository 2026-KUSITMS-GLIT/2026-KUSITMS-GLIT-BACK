package com.groute.groute_server.report.application.port.in;

/**
 * 리포트 생성 화면 진입 시 자동 선택된 심화기록 상세 정보.
 *
 * @param starRecordId 심화기록 PK
 * @param date 스크럼 날짜 (yyyy-MM-dd)
 * @param projectName 프로젝트 태그명
 * @param scrumContent 스크럼 본문
 */
public record AutoSelectedStarRecordView(
        Long starRecordId, String date, String projectName, String scrumContent) {}
