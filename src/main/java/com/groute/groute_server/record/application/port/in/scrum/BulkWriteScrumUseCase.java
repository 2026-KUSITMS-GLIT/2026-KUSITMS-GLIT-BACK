package com.groute.groute_server.record.application.port.in.scrum;

/** 스크럼 일괄 저장 유스케이스 (POST /api/scrums/write). */
public interface BulkWriteScrumUseCase {

    BulkWriteScrumResult bulkWrite(BulkWriteScrumCommand command);
}