package com.groute.groute_server.record.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.StarRecord;

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

    /** 해당 날짜에 미완료 StarRecord가 존재하는지 확인. 모든 STAR 완료 여부 판단에 사용. */
    @Query(
            "SELECT (count(sr) > 0) FROM StarRecord sr "
                    + "WHERE sr.user.id = :userId "
                    + "AND sr.scrum.scrumDate = :date "
                    + "AND sr.isCompleted = false "
                    + "AND sr.isDeleted = false")
    boolean existsIncompleteByUserAndDate(
            @Param("userId") Long userId, @Param("date") LocalDate date);
}
