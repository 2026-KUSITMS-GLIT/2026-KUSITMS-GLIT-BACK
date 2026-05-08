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

    /** 비정규화 카운터(title_count) 증감. increment는 음수 가능. */
    @Modifying
    @Query("UPDATE Project p SET p.titleCount = p.titleCount + :increment WHERE p.id = :id AND p.isDeleted = false")
    int applyTitleCountIncrement(@Param("id") Long id, @Param("increment") int increment);
}
