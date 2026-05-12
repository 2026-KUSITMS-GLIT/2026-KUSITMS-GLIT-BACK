package com.groute.groute_server.report.application.port.out;

import java.util.List;

import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;

/** AI 리포트 생성 요청 포트. FastAPI 서버 준비 전까지 stub 구현체로 동작한다. */
public interface RequestAiReportPort {

    /**
     * AI 서버에 리포트 생성을 요청한다.
     *
     * <p>현재는 stub 구현체로 동작하며, FastAPI 서버 준비 후 실 구현체로 교체한다.
     *
     * @param reportId 생성 대상 리포트 PK
     * @param starRecords 유저가 선택한 심화기록 목록
     * @param scrums 선택된 심화기록 날짜에 속한 스크럼 목록 (자동 수집)
     */
    void requestReportGeneration(Long reportId, List<StarRecord> starRecords, List<Scrum> scrums);
}
