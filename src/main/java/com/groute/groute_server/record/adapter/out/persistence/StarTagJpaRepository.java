package com.groute.groute_server.record.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.application.port.out.star.ScrumStarTagProjection;
import com.groute.groute_server.record.domain.StarTag;

/**
 * STAR 역량 태그(StarTag) JPA 레포지토리.
 *
 * <p>심화기록 상세 응답의 primary 역량 + detail 해시태그 목록을 제공한다. StarTag는 BaseTimeEntity 기반이라 soft-delete 필터 없음
 * — 부모(StarRecord) 삭제 시 JOIN 필터로 자연 차단되는 구조.
 */
public interface StarTagJpaRepository extends JpaRepository<StarTag, Long> {

    @Query("SELECT t FROM StarTag t WHERE t.starRecord.id = :starRecordId ORDER BY t.id ASC")
    List<StarTag> findAllByStarRecordId(@Param("starRecordId") Long starRecordId);

    /**
     * scrumId 집합의 완료 STAR 태그 row(scrumId/primaryCategory/detailTag).
     *
     * <p>userId로 한 번 더 필터(defense-in-depth)하고, soft-delete된 starRecord는 JOIN 조건으로 자연 제외한다. 정렬은
     * {@code st.id ASC}로 안정적.
     */
    @Query(
            "SELECT new com.groute.groute_server.record.application.port.out.star.ScrumStarTagProjection("
                    + "sr.scrum.id, st.primaryCategory, st.detailTag) "
                    + "FROM StarTag st "
                    + "JOIN st.starRecord sr "
                    + "WHERE sr.scrum.id IN :scrumIds "
                    + "  AND sr.user.id = :userId "
                    + "  AND sr.isCompleted = true "
                    + "  AND sr.isDeleted = false "
                    + "ORDER BY st.id ASC")
    List<ScrumStarTagProjection> findCompletedTagsByScrumIds(
            @Param("userId") Long userId, @Param("scrumIds") List<Long> scrumIds);

    /**
     * 해당 사용자가 소유한 모든 StarTag 물리 삭제(MYP-005 hard delete 배치).
     *
     * <p>StarTag엔 user_id 컬럼이 없어 부모 StarRecord를 거쳐 subquery로 매핑한다. 복구 불가, 호출자가 hardDeleteAt 도달 사용자만
     * 전달할 책임. 부모 StarRecord 삭제 전에 호출되어야 FK 위반을 피한다.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query(
            "DELETE FROM StarTag t "
                    + "WHERE t.starRecord.id IN ("
                    + "  SELECT sr.id FROM StarRecord sr WHERE sr.user.id = :userId)")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
