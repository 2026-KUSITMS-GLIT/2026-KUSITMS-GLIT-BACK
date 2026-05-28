package com.groute.groute_server.calendar.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

/**
 * 월별 캘린더 집계용 STAR row projection.
 *
 * <p>StarTag가 있는 StarRecord는 동일 {@code starRecordId}로 1~3 row가 반환될 수 있다. StarTag가 없는 StarRecord(태깅
 * 실패 케이스)는 {@code starRecordId}당 정확히 1 row가 반환되며 이 경우 {@code primaryCategory}는 null이다. 그날 STAR 카운트를
 * 셀 때는 {@code starRecordId} 기준 distinct가 필요하다.
 */
public record StarDailyRow(
        Long starRecordId,
        LocalDate scrumDate,
        OffsetDateTime completedAt,
        CompetencyCategory primaryCategory) {}
