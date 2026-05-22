package com.groute.groute_server.record.application.port.in.scrumtitle;

/**
 * 스크럼 제목(freeText) 단위 일괄 삭제 유스케이스 (DELETE /api/scrums/titles/{titleId}).
 *
 * <p>지정 ScrumTitle 산하의 모든 Scrum을 soft-delete 하고 연결된 STAR·첨부 이미지를 cascade 정리한 뒤 ScrumTitle 자체도
 * soft-delete 한다. 14일 편집 윈도우와 hasStar 거부 정책은 적용하지 않는다.
 */
public interface DeleteScrumTitleUseCase {

    void deleteScrumTitle(DeleteScrumTitleCommand command);
}
