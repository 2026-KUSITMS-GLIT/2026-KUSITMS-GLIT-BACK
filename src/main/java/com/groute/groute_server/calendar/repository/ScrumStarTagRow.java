package com.groute.groute_server.calendar.repository;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

/**
 * 날짜 프리뷰용 STAR 태그 row projection.
 *
 * <p>StarTag가 있는 StarRecord는 동일 {@code scrumId}로 1~3 row가 반환된다. {@code primaryCategory}는 모든 row가 같은
 * 값을 가지며, {@code detailTag}는 row마다 다르다. StarTag가 없는 StarRecord(태깅 실패 케이스)는 1 row가 반환되며 {@code
 * primaryCategory}와 {@code detailTag}는 null이다.
 */
public record ScrumStarTagRow(Long scrumId, CompetencyCategory primaryCategory, String detailTag) {}
