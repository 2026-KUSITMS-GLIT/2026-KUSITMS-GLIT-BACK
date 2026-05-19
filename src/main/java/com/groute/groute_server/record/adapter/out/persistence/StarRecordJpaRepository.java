package com.groute.groute_server.record.adapter.out.persistence;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.application.port.out.star.CompetencyCount;
import com.groute.groute_server.record.domain.StarRecord;
import com.groute.groute_server.record.domain.enums.StarRecordStatus;

/**
 * 심화 STAR 기록(StarRecord) JPA 레포지토리.
 *
 * <p>스크럼 sync 시 cascade soft-delete (CAL-002), 단건 상세 조회·삭제 (CAL-003)에 사용한다.
 */
public interface StarRecordJpaRepository extends JpaRepository<StarRecord, Long> {

    /** 해당 스크럼에 살아있는 STAR 기록이 있는지. */
    @Query(
            "SELECT (count(sr) > 0) FROM StarRecord sr "
                    + "WHERE sr.scrum.id = :scrumId AND sr.isDeleted = false")
    boolean existsByScrumId(@Param("scrumId") Long scrumId);

    /** Scrum cascade soft-delete. 동일 트랜잭션에서 Scrum 삭제와 함께 호출. */
    @Modifying
    @Query(
            "UPDATE StarRecord sr "
                    + "SET sr.isDeleted = true, sr.deletedAt = CURRENT_TIMESTAMP "
                    + "WHERE sr.scrum.id IN :scrumIds AND sr.isDeleted = false")
    int deleteAllByScrumIdIn(@Param("scrumIds") Collection<Long> scrumIds);

    /** 논리 삭제된 레코드를 제외한 단건 조회. */
    Optional<StarRecord> findByIdAndIsDeletedFalse(Long id);

    /** 이미지 업로드 시 2장 제한 Race Condition 방지용 비관적 락 조회. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sr FROM StarRecord sr WHERE sr.id = :id AND sr.isDeleted = false")
    Optional<StarRecord> findByIdWithLock(@Param("id") Long id);

    /** 단건 상세 조회. 응답 카테고리·부제목 매핑을 위해 Scrum·Title·Project까지 fetch join. */
    @Query(
            "SELECT sr FROM StarRecord sr "
                    + "JOIN FETCH sr.scrum s "
                    + "JOIN FETCH s.title t "
                    + "JOIN FETCH t.project "
                    + "WHERE sr.id = :id AND sr.isDeleted = false")
    Optional<StarRecord> findByIdWithScrum(@Param("id") Long id);

    /** 단건 soft-delete. cascade(Scrum.hasStar=false 등)는 호출자가 별도 처리. */
    @Modifying
    @Query(
            "UPDATE StarRecord sr "
                    + "SET sr.isDeleted = true, sr.deletedAt = CURRENT_TIMESTAMP "
                    + "WHERE sr.id = :id AND sr.isDeleted = false")
    int softDeleteById(@Param("id") Long id);

    /** 사용자의 TAGGED 심화기록 총 개수. 홈 요약(isFirstStar·reportModal) 판단에 사용. */
    @Query(
            "SELECT COUNT(sr) FROM StarRecord sr "
                    + "WHERE sr.user.id = :userId "
                    + "AND sr.status = :status "
                    + "AND sr.isDeleted = false")
    long countTaggedByUserId(
            @Param("userId") Long userId, @Param("status") StarRecordStatus status);

    /** TAGGED 완료된 STAR 기록의 역량 카테고리별 건수. selectedCompetency가 NULL인 스크럼은 제외. */
    @Query(
            "SELECT new com.groute.groute_server.record.application.port.out.star.CompetencyCount("
                    + "s.selectedCompetency, COUNT(sr)) "
                    + "FROM StarRecord sr JOIN sr.scrum s "
                    + "WHERE sr.user.id = :userId "
                    + "AND sr.status = :status "
                    + "AND sr.isDeleted = false "
                    + "AND s.selectedCompetency IS NOT NULL "
                    + "GROUP BY s.selectedCompetency")
    List<CompetencyCount> countCompletedByCompetency(
            @Param("userId") Long userId, @Param("status") StarRecordStatus status);

    /** 해당 날짜에 TAGGED 미완료 StarRecord가 존재하는지 확인. */
    @Query(
            "SELECT (COUNT(sr) > 0) FROM StarRecord sr "
                    + "JOIN sr.scrum s "
                    + "WHERE sr.user.id = :userId "
                    + "AND s.scrumDate = :date "
                    + "AND sr.status <> :tagged "
                    + "AND sr.isDeleted = false")
    boolean existsUntaggedByUserAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("tagged") StarRecordStatus tagged);

    /**
     * 특정 시점 이후 완료된 심화기록 수를 카운트한다.
     *
     * <p>after가 null이면 전체 완료된 심화기록 수를 반환한다 (신규 유저 케이스).
     */
    @Query(
            "SELECT COUNT(sr) FROM StarRecord sr "
                    + "WHERE sr.user.id = :userId "
                    + "AND sr.isCompleted = true "
                    + "AND sr.isDeleted = false "
                    + "AND (:after IS NULL OR sr.completedAt > :after)")
    int countCompletedAfter(@Param("userId") Long userId, @Param("after") OffsetDateTime after);

    /**
     * 해당 사용자가 소유한 모든 StarRecord 물리 삭제(MYP-005 hard delete 배치).
     *
     * <p>자식 테이블(StarTag/StarImage/AiTaggingJob)이 먼저 정리되어야 FK 위반을 피한다. soft-delete 여부와 무관하게 모든 row
     * 삭제. 복구 불가.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query("DELETE FROM StarRecord sr WHERE sr.user.id = :userId")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
