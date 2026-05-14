package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.record.domain.Project;

interface ProjectJpaRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

    Page<Project> findAllByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    boolean existsByUserIdAndNameAndIsDeletedFalse(Long userId, String name);

    /**
     * 해당 사용자가 소유한 모든 Project 물리 삭제(MYP-005 hard delete 배치).
     *
     * <p>자식(ScrumTitle) 정리 후 호출되어야 FK 위반을 피한다. soft-delete 여부와 무관하게 모든 row 삭제. 복구 불가.
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query("DELETE FROM Project p WHERE p.user.id = :userId")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
