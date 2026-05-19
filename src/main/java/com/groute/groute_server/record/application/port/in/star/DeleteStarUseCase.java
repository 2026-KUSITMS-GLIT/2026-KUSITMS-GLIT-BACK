package com.groute.groute_server.record.application.port.in.star;

/**
 * 심화기록 단독 삭제 유스케이스 (CAL-003, DELETE /api/star-records/{starRecordId}).
 *
 * <p>STAR soft-delete + 연결된 Scrum의 hasStar 플래그를 false로 동기화한다. 스크럼 본문은 보존.
 */
public interface DeleteStarUseCase {

    void deleteStar(DeleteStarCommand command);
}
