package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
