package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.groute.groute_server.record.domain.Project;

interface ProjectJpaRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

    Page<Project> findAllByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    boolean existsByUserIdAndNameAndIsDeletedFalse(Long userId, String name);
}