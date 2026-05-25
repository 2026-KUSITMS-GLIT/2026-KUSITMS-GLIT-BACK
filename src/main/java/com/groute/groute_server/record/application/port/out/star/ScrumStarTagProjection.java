package com.groute.groute_server.record.application.port.out.star;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

/**
 * 스크럼당 완료 STAR 태그 row.
 *
 * <p>한 Scrum(=StarRecord 1:1)에 1~3개의 row가 생성된다. {@code starRecordId}와 {@code primaryCategory}는 같은
 * scrumId 내에서 동일 값이며 {@code detailTag}는 row마다 다르다. 호출 측이 distinct·그룹 집계를 책임진다.
 */
public record ScrumStarTagProjection(
        Long scrumId, Long starRecordId, CompetencyCategory primaryCategory, String detailTag) {}
