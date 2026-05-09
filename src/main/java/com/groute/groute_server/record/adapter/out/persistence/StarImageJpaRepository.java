package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.StarImage;

/**
 * STAR 첨부 이미지(StarImage) JPA 레포지토리.
 *
 * <p>심화기록 상세 응답의 이미지 목록을 sortOrder 오름차순으로 제공한다. StarImage는 BaseTimeEntity 기반이라 soft-delete 필터 없음.
 */
public interface StarImageJpaRepository extends JpaRepository<StarImage, Long> {

    @Query(
            "SELECT i FROM StarImage i "
                    + "WHERE i.starRecord.id = :starRecordId "
                    + "ORDER BY i.sortOrder ASC")
    List<StarImage> findAllByStarRecordIdOrderBySortOrderAsc(
            @Param("starRecordId") Long starRecordId);

    @Query("SELECT i FROM StarImage i WHERE i.starRecord.scrum.id IN :scrumIds")
    List<StarImage> findAllByStarRecordScrumIdIn(@Param("scrumIds") Collection<Long> scrumIds);

    /**
     * 해당 사용자가 소유한 모든 StarImage 물리 삭제(MYP-005 hard delete 배치).
     *
     * <p>StarImage엔 user_id 컬럼이 없어 부모 StarRecord를 거쳐 subquery로 매핑한다. 외부 스토리지의 원본 파일 정리는 별도 port에서
     * 선행되어야 하며, 본 메서드는 DB row만 정리한다. 복구 불가.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query(
            "DELETE FROM StarImage i "
                    + "WHERE i.starRecord.id IN ("
                    + "  SELECT sr.id FROM StarRecord sr WHERE sr.user.id = :userId)")
    int hardDeleteAllByUserId(@Param("userId") Long userId);

    /**
     * 해당 사용자가 소유한 StarImage의 image_key 목록 조회(hard delete 배치 S3 정리용).
     *
     * <p>DB row 삭제 전에 S3 삭제 대상 키를 수집하기 위한 read-only 쿼리. image_key가 null인 row는 제외(레거시 데이터).
     */
    @Query(
            "SELECT i.imageKey FROM StarImage i "
                    + "WHERE i.imageKey IS NOT NULL "
                    + "AND i.starRecord.id IN ("
                    + "  SELECT sr.id FROM StarRecord sr WHERE sr.user.id = :userId)")
    List<String> findAllImageKeysByUserId(@Param("userId") Long userId);
}
