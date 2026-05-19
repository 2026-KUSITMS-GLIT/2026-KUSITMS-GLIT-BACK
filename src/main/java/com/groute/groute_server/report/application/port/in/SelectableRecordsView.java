package com.groute.groute_server.report.application.port.in;

import java.util.List;

/**
 * 날짜별 심화기록 모달 조회 응답 모델.
 *
 * @param date 조회 날짜 (yyyy-MM-dd)
 * @param starRecords 해당 날짜에 완료된 심화기록 목록
 */
public record SelectableRecordsView(String date, List<StarRecordItem> starRecords) {

    /**
     * 심화기록 항목.
     *
     * @param starRecordId 심화기록 식별자
     * @param projectName 프로젝트명
     * @param scrumContent 해당 심화기록이 속한 스크럼 본문
     */
    public record StarRecordItem(Long starRecordId, String projectName, String scrumContent) {}
}
