package com.groute.groute_server.report.application.port.in;

/** 리포트 생성용 사전 정보 조회 유스케이스. 미니/커리어 타입 판단 및 달력 렌더링에 필요한 정보를 반환한다. */
public interface GetSelectableInfoUseCase {

    SelectableInfoView getSelectableInfo(Long userId);
}
