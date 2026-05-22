package com.groute.groute_server.record.application.port.in.scrum;

/** 스크럼 단일 삭제 입력. */
public record DeleteScrumCommand(Long userId, Long scrumId) {}
