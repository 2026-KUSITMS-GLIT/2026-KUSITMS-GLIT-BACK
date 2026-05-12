package com.groute.groute_server.report.application.port.in;

/** 리포트 생성 요청 유스케이스. 유저가 선택한 심화기록을 바탕으로 AI 서버에 리포트 생성을 요청한다. */
public interface CreateReportUseCase {

    /** @return 생성된 리포트 ID */
    Long createReport(CreateReportCommand command);
}
