package com.groute.groute_server.record.application.port.in.scrum;

/** 스크럼 역량 일괄 선택 유스케이스 (PATCH /api/scrums/competencies). */
public interface UpdateScrumCompetencyUseCase {

    void updateCompetency(UpdateScrumCompetencyCommand command);
}