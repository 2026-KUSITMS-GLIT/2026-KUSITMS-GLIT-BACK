package com.groute.groute_server.record.application.port.in.scrum;

/**
 * 스크럼 단일 삭제 유스케이스 (DELETE /api/scrums/{scrumId}).
 *
 * <p>Scrum soft-delete + 연결된 STAR·첨부 이미지 cascade 정리 + ScrumTitle.scrumCount 1 감소. 14일 편집 윈도우와
 * hasStar 거부 정책은 적용하지 않는다.
 */
public interface DeleteScrumUseCase {

    void deleteScrum(DeleteScrumCommand command);
}
