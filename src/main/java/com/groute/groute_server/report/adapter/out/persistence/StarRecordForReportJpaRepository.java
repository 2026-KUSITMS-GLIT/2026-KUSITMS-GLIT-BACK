package com.groute.groute_server.report.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.StarRecord;

/**
 * 리포트 도메인에서 사용하는 심화기록 조회 전용 JPA 레포지토리.
 *
 * <p>record 도메인 JPA 레포지토리를 직접 참조하지 않고 별도 레포지토리를 두어 도메인 경계를 유지한다.
 */
public interface StarRecordForReportJpaRepository extends JpaRepository<StarRecord, Long> {

    /** 유저의 완료된 심화기록 수를 반환한다. */
    @Query(
            "SELECT COUNT(sr) FROM StarRecord sr "
                    + "WHERE sr.user.id = :userId "
                    + "AND sr.isCompleted = true "
                    + "AND sr.isDeleted = false")
    int countCompletedByUserId(@Param("userId") Long userId);

    /**
     * 유저의 완료된 심화기록을 최신순으로 반환한다.
     *
     * <p>같은 날짜 내 여러 심화기록은 id 기준 내림차순(기록 순서 가장 나중 것부터) 산정한다.
     */
    @Query(
            "SELECT sr FROM StarRecord sr "
                    + "WHERE sr.user.id = :userId "
                    + "AND sr.isCompleted = true "
                    + "AND sr.isDeleted = false "
                    + "ORDER BY sr.scrum.scrumDate DESC, sr.id DESC "
                    + "LIMIT :limit")
    List<StarRecord> findCompletedByUserIdOrderByLatest(
            @Param("userId") Long userId, @Param("limit") int limit);

    /** 완료된 심화기록이 있는 날짜 목록을 전체 기간 반환한다. 달력 하이라이트 렌더링용. */
    @Query(
            "SELECT DISTINCT sr.scrum.scrumDate FROM StarRecord sr "
                    + "WHERE sr.user.id = :userId "
                    + "AND sr.isCompleted = true "
                    + "AND sr.isDeleted = false "
                    + "ORDER BY sr.scrum.scrumDate DESC")
    List<java.time.LocalDate> findCompletedStarDatesByUserId(@Param("userId") Long userId);

    /** 심화기록 ID 목록으로 심화기록을 조회한다. userId로 소유권을 함께 검증한다. */
    @Query(
            "SELECT sr FROM StarRecord sr "
                    + "WHERE sr.user.id = :userId "
                    + "AND sr.id IN :ids "
                    + "AND sr.isDeleted = false")
    List<StarRecord> findAllByIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    /**
     * 유저의 특정 날짜에 완료된 심화기록 목록을 기록 순서 기준 오름차순으로 반환한다.
     *
     * <p>projectName 조회를 위해 scrum → title → project 경로를 fetch join으로 한 번에 로드한다.
     */
    @Query(
            "SELECT sr FROM StarRecord sr "
                    + "JOIN FETCH sr.scrum s "
                    + "JOIN FETCH s.title t "
                    + "JOIN FETCH t.project p "
                    + "WHERE sr.user.id = :userId "
                    + "AND s.scrumDate = :date "
                    + "AND sr.isCompleted = true "
                    + "AND sr.isDeleted = false "
                    + "ORDER BY sr.id ASC")
    List<StarRecord> findCompletedByUserIdAndDate(
            @Param("userId") Long userId, @Param("date") LocalDate date);
}
