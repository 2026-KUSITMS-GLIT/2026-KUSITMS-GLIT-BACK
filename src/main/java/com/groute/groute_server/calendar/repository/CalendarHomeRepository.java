package com.groute.groute_server.calendar.repository;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * 캘린더 메인 홈 조회 전용 Repository.
 *
 * <p>record 도메인 entity({@code Scrum}, {@code StarRecord}, {@code StarTag})를 read-only로 조회한다. 쓰기 로직은
 * record 도메인 책임이며, 본 클래스는 read 전용이다.
 *
 * <p>scrum_date는 {@code LocalDate}로 저장되어 별도 timezone 변환이 필요 없다. STAR 완료된 day의 대표 역량은 같은
 * starRecord에서 1~3개의 StarTag row가 나오므로, 호출 측에서 starRecordId 기준 distinct 처리 후 가장 최근 완료 record의
 * primaryCategory를 사용한다.
 */
@Repository
@RequiredArgsConstructor
public class CalendarHomeRepository {

    private final EntityManager em;

    /** 사용자가 해당 기간에 스크럼을 작성한 날짜 set. */
    public List<LocalDate> findScrumDatesInRange(Long userId, LocalDate start, LocalDate end) {
        return em.createQuery(
                        """
                        SELECT DISTINCT s.scrumDate
                        FROM Scrum s
                        WHERE s.user.id = :userId
                          AND s.scrumDate BETWEEN :start AND :end
                        """,
                        LocalDate.class)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    /**
     * 해당 기간에 완료된 STAR 기록의 day-level row.
     *
     * <p>StarTag join으로 starRecord 1개당 1~3 row가 나온다. 호출 측에서 starRecordId 기준 distinct로 카운트하고, 동일
     * record의 primaryCategory는 모든 row가 같은 값을 가진다.
     */
    public List<StarDailyRow> findCompletedStarRowsInRange(
            Long userId, LocalDate start, LocalDate end) {
        return em.createQuery(
                        """
                        SELECT new com.groute.groute_server.calendar.repository.StarDailyRow(
                            sr.id, sr.scrum.scrumDate, sr.completedAt, st.primaryCategory)
                        FROM StarTag st
                        JOIN st.starRecord sr
                        WHERE sr.user.id = :userId
                          AND sr.isCompleted = true
                          AND sr.scrum.scrumDate BETWEEN :start AND :end
                        """,
                        StarDailyRow.class)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }
}
