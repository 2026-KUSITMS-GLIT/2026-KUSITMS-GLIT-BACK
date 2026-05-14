package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.ScrumTitle;

/**
 * 스크럼 제목(ScrumTitle) JPA 레포지토리.
 *
 * <p>스크럼 sync API에서 요청 titleId의 일괄 소유권 검증에 사용한다. 모든 조회는 {@code is_deleted = false} 기준.
 */
public interface ScrumTitleJpaRepository extends JpaRepository<ScrumTitle, Long> {

    /** 요청 titleId 집합 중 본인 소유인 것만 반환. 결과 크기로 미존재/타인 소유를 판별한다. */
    @Query(
            "SELECT t FROM ScrumTitle t "
                    + "WHERE t.id IN :ids AND t.user.id = :userId AND t.isDeleted = false")
    List<ScrumTitle> findAllByIdInAndUserId(
            @Param("ids") Collection<Long> ids, @Param("userId") Long userId);

    /** 비정규화 카운터(scrum_count) 증감. increment는 음수 가능. */
    @Modifying
    @Query(
            "UPDATE ScrumTitle t SET t.scrumCount = t.scrumCount + :increment "
                    + "WHERE t.id = :id AND t.isDeleted = false")
    int applyScrumCountIncrement(@Param("id") Long id, @Param("increment") int increment);

    /**
     * 해당 사용자가 소유한 모든 ScrumTitle 물리 삭제(MYP-005 hard delete 배치).
     *
     * <p>자식(Scrum) 정리 후, 부모(Project) 정리 전에 호출되어야 FK 위반을 피한다. soft-delete 여부와 무관하게 모든 row 삭제. 복구 불가.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query("DELETE FROM ScrumTitle t WHERE t.user.id = :userId")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
