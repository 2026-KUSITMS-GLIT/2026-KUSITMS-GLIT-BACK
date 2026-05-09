package com.groute.groute_server.home.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 홈 역량 잔디 조회 응답(HOM-002).
 *
 * <p>월 단위 히트맵용. 요청 월의 모든 일자를 {@code days}로 반환하며, 데이터 없는 날도 NO_DATA로 포함한다(FE 합의).
 *
 * <p>일자별 색상 농도는 STAR 완료 건수 기준 4단계 매핑(0=NO_DATA, 1=LOW, 2=MID, 3+=HIGH). 기획상 "스크럼만 있는 상태"가 발생하지 않게
 * 변경되어 SCRUM_ONLY는 산출 대상에서 제외된다.
 */
@Schema(description = "홈 역량 잔디 조회 응답")
public record CompetencyStatsResponse(
        @JsonFormat(pattern = "yyyy-MM")
                @Schema(description = "요청한 월", example = "2026-04", type = "string")
                YearMonth month,
        @Schema(description = "일별 데이터 (요청 월의 모든 일자 포함)") List<DayItem> days) {

    /**
     * 사용자별 STAR 완료 건수 집계 결과로부터 응답을 생성한다.
     *
     * <p>{@code completedCountsByDate}는 STAR 완료 1건 이상인 일자만 키로 갖는 sparse map. 빠진 일자는 NO_DATA로 채워 요청
     * 월의 1일~말일을 빠짐없이 포함시킨다.
     *
     * @param month 요청 월
     * @param completedCountsByDate 일자별 STAR 완료 건수 (없는 일자 키 자체가 누락된 sparse map). null 금지.
     */
    public static CompetencyStatsResponse from(
            YearMonth month, Map<LocalDate, Long> completedCountsByDate) {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(completedCountsByDate, "completedCountsByDate");
        List<DayItem> days =
                month.atDay(1)
                        .datesUntil(month.atEndOfMonth().plusDays(1))
                        .map(
                                date ->
                                        new DayItem(
                                                date,
                                                DayStatus.fromCount(
                                                        completedCountsByDate.getOrDefault(
                                                                date, 0L))))
                        .toList();
        return new CompetencyStatsResponse(month, days);
    }

    @Schema(description = "일별 항목")
    public record DayItem(
            @Schema(description = "일자", example = "2026-04-01", type = "string", format = "date")
                    LocalDate date,
            @Schema(description = "일별 상태", example = "STAR_LOW") DayStatus status) {}

    /**
     * 일별 역량 잔디 상태.
     *
     * <p>해당 일자의 STAR 완료 건수에 따라 히트맵 색상 농도를 차등 적용하기 위한 상태값.
     */
    public enum DayStatus {
        /** STAR 완료 0건. */
        NO_DATA,
        /** STAR 완료 1건. */
        STAR_LOW,
        /** STAR 완료 2건. */
        STAR_MID,
        /** STAR 완료 3건 이상. */
        STAR_HIGH;

        /**
         * 일자별 STAR 완료 건수를 상태 enum으로 매핑한다.
         *
         * <p>임계치: 0건=NO_DATA, 1건=LOW, 2건=MID, 3건 이상=HIGH. 음수는 방어적으로 NO_DATA로 처리한다.
         */
        public static DayStatus fromCount(long completedStarCount) {
            if (completedStarCount <= 0) {
                return NO_DATA;
            }
            if (completedStarCount == 1) {
                return STAR_LOW;
            }
            if (completedStarCount == 2) {
                return STAR_MID;
            }
            return STAR_HIGH;
        }
    }
}
