package com.groute.groute_server.calendar.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.groute.groute_server.record.domain.enums.CompetencyCategory;

/**
 * 월별 캘린더 집계용 STAR row projection.
 *
 * <p>한 StarRecord에 StarTag가 1~3개 있으므로 동일 {@code starRecordId}로 1~3 row가 반환될 수 있다. 그날 STAR 카운트를 셀 때는
 * {@code starRecordId} 기준 distinct가 필요하며, 대표 역량은 한 record 내 모든 row가 같은 값을 가진다.
 */
public record StarDailyRow(
        Long starRecordId,
        LocalDate scrumDate,
        OffsetDateTime completedAt,
        CompetencyCategory primaryCategory) {}
