package com.groute.groute_server.report.adapter.out.ai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.groute.groute_server.record.domain.Scrum;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.report.application.port.out.RequestAiReportPort;

/**
 * {@link RequestAiReportPort}의 stub 구현체.
 *
 * <p>FastAPI AI 서버 준비 전까지 로그만 남기고 아무 동작도 하지 않는다. AI 서버 연동 시 실 구현체로 교체한다.
 */
@Component
public class AiReportStubAdapter implements RequestAiReportPort {

    private static final Logger log = LoggerFactory.getLogger(AiReportStubAdapter.class);

    @Override
    public void requestReportGeneration(
            Long reportId, List<StarRecord> starRecords, List<Scrum> scrums) {
        log.info(
                "[STUB] AI 리포트 생성 요청 — reportId={}, starRecords={}, scrums={}",
                reportId,
                starRecords.size(),
                scrums.size());
    }
}
