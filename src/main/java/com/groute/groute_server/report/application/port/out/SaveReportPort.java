package com.groute.groute_server.report.application.port.out;

import com.groute.groute_server.report.domain.Report;

/** 리포트 저장 포트. 신규 생성 및 상태 변경 모두 여기서 처리한다. */
public interface SaveReportPort {

    /**
     * 리포트를 저장한다. 신규 생성 및 상태 변경 모두 사용한다.
     *
     * @param report 저장할 리포트
     * @return 저장된 리포트
     */
    Report save(Report report);
}
