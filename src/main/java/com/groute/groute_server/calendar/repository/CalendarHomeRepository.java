package com.groute.groute_server.calendar.repository;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.groute.groute_server.record.domain.Scrum;

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
 *
 * <p><b>Soft-delete 처리</b>: {@link com.groute.groute_server.common.entity.SoftDeleteEntity}는
 * {@code @Where} 자동 필터를 걸지 않으므로, 모든 JPQL에 명시적으로 {@code AND s.isDeleted = false} 조건을 추가한다(record 도메인
 * 쿼리와 동일 컨벤션). StarTag는 BaseTimeEntity를 상속하여 자체 soft-delete 필드가 없으며, 부모 StarRecord의 {@code
 * isDeleted=false}로 보호된다.
 */
@Repository
@RequiredArgsConstructor
public class CalendarHomeRepository {

    private final EntityManager em;

    /** 사용자가 해당 기간에 스크럼을 작성한 날짜 set. soft-delete된 scrum은 제외. */
    public List<LocalDate> findScrumDatesInRange(Long userId, LocalDate start, LocalDate end) {
        return em.createQuery(
                        """
                        SELECT DISTINCT s.scrumDate
                        FROM Scrum s
                        WHERE s.user.id = :userId
                          AND s.scrumDate BETWEEN :start AND :end
                          AND s.isDeleted = false
                        ORDER BY s.scrumDate
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
     * record의 primaryCategory는 모든 row가 같은 값을 가진다. soft-delete된 starRecord는 제외.
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
                          AND sr.isDeleted = false
                          AND sr.scrum.scrumDate BETWEEN :start AND :end
                        ORDER BY sr.scrum.scrumDate, sr.completedAt
                        """,
                        StarDailyRow.class)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    /**
     * 지정 일자의 사용자 스크럼 목록. ScrumTitle·Project를 fetch join 하여 추가 쿼리를 피한다.
     *
     * <p>soft-delete된 scrum은 제외. (title/project는 부모 scrum이 살아있으면 동시 살아있다고 가정하는 record 도메인 컨벤션을 따라
     * 추가 필터하지 않는다.)
     */
    public List<Scrum> findScrumsByUserAndDate(Long userId, LocalDate date) {
        return em.createQuery(
                        """
                        SELECT s
                        FROM Scrum s
                        JOIN FETCH s.title t
                        JOIN FETCH t.project
                        WHERE s.user.id = :userId
                          AND s.scrumDate = :date
                          AND s.isDeleted = false
                        ORDER BY s.id
                        """,
                        Scrum.class)
                .setParameter("userId", userId)
                .setParameter("date", date)
                .getResultList();
    }

    /**
     * 지정 scrumId 집합 중 STAR가 완료된 record의 StarTag row 목록.
     *
     * <p>한 StarRecord(=Scrum 1:1)에 1~3개의 row가 반환된다. 정렬은 {@code st.id ASC}로 안정적. soft-delete된
     * starRecord는 제외. 호출부가 본인 scrumIds만 전달한다고 신뢰하지 않고 저장소 단에서도 {@code userId}로 한 번 더
     * 강제(defense-in-depth).
     */
    public List<ScrumStarTagRow> findCompletedStarTagsByScrumIds(Long userId, List<Long> scrumIds) {
        if (scrumIds.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                        """
                        SELECT new com.groute.groute_server.calendar.repository.ScrumStarTagRow(
                            sr.scrum.id, st.primaryCategory, st.detailTag)
                        FROM StarTag st
                        JOIN st.starRecord sr
                        WHERE sr.scrum.id IN :scrumIds
                          AND sr.user.id = :userId
                          AND sr.isCompleted = true
                          AND sr.isDeleted = false
                        ORDER BY st.id
                        """,
                        ScrumStarTagRow.class)
                .setParameter("userId", userId)
                .setParameter("scrumIds", scrumIds)
                .getResultList();
    }
}
