package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
