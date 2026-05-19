package com.groute.groute_server.record.domain;

import java.util.List;

/** 프로젝트 태그 페이지 조회 결과 (Spring Data 비의존 도메인 타입). */
public record ProjectPage(List<Project> content, int page, int size, int totalPages) {}
