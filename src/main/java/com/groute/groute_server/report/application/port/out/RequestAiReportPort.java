package com.groute.groute_server.report.application.port.out;

import java.util.List;
import java.util.Map;

import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;

/** AI 리포트 생성 요청 포트. */
public interface RequestAiReportPort {

    /**
     * AI 서버에 리포트 생성을 요청하고 결과를 반환한다.
     *
     * @param reportId 생성 대상 리포트 PK
     * @param starRecords 유저가 선택한 심화기록 목록
     * @param scrums 선택된 심화기록 날짜에 속한 스크럼 목록
     * @return AI 응답 결과 (title, contentJson)
     */
    AiReportResult requestReportGeneration(
            Long reportId, List<StarRecord> starRecords, List<Scrum> scrums);

    /** AI 응답 결과를 담는 레코드. */
    record AiReportResult(String title, Map<String, Object> contentJson) {}
}
