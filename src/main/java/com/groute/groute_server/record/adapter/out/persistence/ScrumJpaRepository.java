package com.groute.groute_server.record.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.Scrum;

/**
 * 데일리 스크럼(Scrum) JPA 레포지토리.
 *
 * <p>일자별 조회 시 ScrumTitle·Project를 fetch join 하여 N+1을 회피한다. 모든 조회는 {@code is_deleted = false} 기준.
 */
public interface ScrumJpaRepository extends JpaRepository<Scrum, Long> {

    /** 일자별 사용자 스크럼 전체. 응답 그룹핑을 위해 titleId·id 오름차순 고정. */
    @Query(
            "SELECT s FROM Scrum s "
                    + "JOIN FETCH s.title t "
                    + "JOIN FETCH t.project "
                    + "WHERE s.user.id = :userId "
                    + "AND s.scrumDate = :date "
                    + "AND s.isDeleted = false "
                    + "ORDER BY t.id ASC, s.id ASC")
    List<Scrum> findAllByUserIdAndScrumDate(
            @Param("userId") Long userId, @Param("date") LocalDate date);

    /** 요청 scrumId 집합 중 본인 소유인 것만 반환. 결과 크기로 미존재/타인 소유를 판별한다. */
    @Query(
            "SELECT s FROM Scrum s "
                    + "WHERE s.id IN :ids AND s.user.id = :userId AND s.isDeleted = false")
    List<Scrum> findAllByIdInAndUserId(
            @Param("ids") Collection<Long> ids, @Param("userId") Long userId);

    /** 본문 변경. 호출자가 14일·hasStar 검증 선행. */
    @Modifying
    @Query("UPDATE Scrum s SET s.content = :content " + "WHERE s.id = :id AND s.isDeleted = false")
    int updateContent(@Param("id") Long id, @Param("content") String content);

    /** soft-delete. cascade는 호출자가 별도 처리. */
    @Modifying
    @Query(
            "UPDATE Scrum s SET s.isDeleted = true, s.deletedAt = CURRENT_TIMESTAMP "
                    + "WHERE s.id IN :ids AND s.isDeleted = false")
    int softDeleteAllByIdIn(@Param("ids") Collection<Long> ids);

    /** STAR 단독 삭제 시 Scrum.hasStar 플래그 해제. */
    @Modifying
    @Query("UPDATE Scrum s SET s.hasStar = false " + "WHERE s.id = :id AND s.isDeleted = false")
    int clearHasStarById(@Param("id") Long id);

    /**
     * 후보 user 중 KST 기준 해당 일자에 스크럼을 1개 이상 작성한 user_id 집합(MYP-004 알림 발송 시 작성자 제외 처리).
     *
     * <p>{@code scrum_date}는 사용자가 선택한 KST 기준 날짜라 DB UTC 변환과 무관하게 직접 비교 가능. {@code is_deleted =
     * false}인 행만 카운트.
     */
    @Query(
            "SELECT DISTINCT s.user.id FROM Scrum s "
                    + "WHERE s.user.id IN :userIds "
                    + "AND s.scrumDate = :date "
                    + "AND s.isDeleted = false")
    List<Long> findDistinctUserIdsByScrumDate(
            @Param("userIds") Collection<Long> userIds, @Param("date") LocalDate date);
}
