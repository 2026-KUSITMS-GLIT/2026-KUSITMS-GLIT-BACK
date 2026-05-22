package com.groute.groute_server.record.application.port.in.scrumtitle;

/** 스크럼 제목(freeText) 단위 일괄 삭제 입력. */
public record DeleteScrumTitleCommand(Long userId, Long titleId) {}
