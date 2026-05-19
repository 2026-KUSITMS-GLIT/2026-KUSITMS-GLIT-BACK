package com.groute.groute_server.report.application.port.in;

import java.time.LocalDate;

/** 날짜별 심화기록 모달 조회 유스케이스. 리포트 생성 화면에서 날짜 셀 클릭 시 모달에 노출할 심화기록 목록을 반환한다. */
public interface GetSelectableRecordsUseCase {

    SelectableRecordsView getSelectableRecords(Long userId, LocalDate date);
}
