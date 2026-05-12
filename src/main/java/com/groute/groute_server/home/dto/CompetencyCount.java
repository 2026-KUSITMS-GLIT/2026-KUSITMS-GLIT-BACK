package com.groute.groute_server.home.dto;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

/** JPQL new 표현식 프로젝션 — 역량 카테고리별 TAGGED STAR 건수. */
public record CompetencyCount(CompetencyCategory competency, Long count) {}
