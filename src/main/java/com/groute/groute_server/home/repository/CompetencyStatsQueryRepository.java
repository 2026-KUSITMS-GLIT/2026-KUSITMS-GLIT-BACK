package com.groute.groute_server.home.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.StarRecord;

/**
 * 홈 역량 잔디 조회 전용 repository(HOM-002).
 *
 * <p>{@code home}은 read-only Layered 모듈이므로 {@link Repository} 마커만 상속해 save/delete 노출을 차단한다. JPQL
 * 인터페이스 프로젝션으로 일자별 STAR 완료 건수 집계만 반환한다.
 *
 * <p>스크럼 일자({@code Scrum.scrumDate})는 사용자가 선택한 KST 기준 {@link LocalDate}라 DB UTC 타임존과 무관하게 직접 비교 가능.
 * STAR 완료 판정은 {@code is_completed=true} + soft-delete가 아닌 행만 카운트한다.
 *
 * <p>인덱스는 Flyway 마이그레이션이 단일 SoT이므로 본 repository에는 선언하지 않는다.
 */
public interface CompetencyStatsQueryRepository extends Repository<StarRecord, Long> {

    /**
     * 사용자의 STAR 완료 기록을 일자별로 카운트한다.
     *
     * <p>기간은 {@code [startInclusive, endExclusive)} 반열림 구간. 호출자(service)가 월 첫날과 다음달 첫날로 변환해 전달하여 말일
     * 경계를 안전하게 포함시킨다. 결과 행은 STAR 완료 1건 이상인 일자만 포함하며, 0건 일자는 키 자체가 누락된 sparse 결과다(서비스 단에서 NO_DATA
     * 채움).
     *
     * @param userId 조회 대상 사용자 ID
     * @param startInclusive 시작 일자(포함, KST)
     * @param endExclusive 종료 일자(미포함, KST)
     * @return 일자 + 완료 건수 행 목록 (0건 일자는 미포함)
     */
    @Query(
            "SELECT s.scrumDate AS date, COUNT(sr.id) AS count "
                    + "FROM StarRecord sr "
                    + "JOIN sr.scrum s "
                    + "WHERE sr.user.id = :userId "
                    + "AND sr.isCompleted = true "
                    + "AND sr.isDeleted = false "
                    + "AND s.isDeleted = false "
                    + "AND s.scrumDate >= :startInclusive "
                    + "AND s.scrumDate < :endExclusive "
                    + "GROUP BY s.scrumDate")
    List<DateCountRow> findCompletedStarCountsByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startInclusive") LocalDate startInclusive,
            @Param("endExclusive") LocalDate endExclusive);

    /** 일자별 STAR 완료 건수 행. JPQL alias({@code date}, {@code count})로 매핑되는 인터페이스 프로젝션. */
    interface DateCountRow {
        LocalDate getDate();

        Long getCount();
    }
}
