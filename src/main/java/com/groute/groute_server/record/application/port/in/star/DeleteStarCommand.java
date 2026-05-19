package com.groute.groute_server.record.application.port.in.star;

/** 심화기록 단독 삭제 입력. */
public record DeleteStarCommand(Long userId, Long starRecordId) {}
