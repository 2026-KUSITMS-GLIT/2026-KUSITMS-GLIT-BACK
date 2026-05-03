package com.groute.groute_server.record.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.groute.groute_server.record.application.port.out.ProjectPort;
import com.groute.groute_server.record.domain.Project;

import lombok.RequiredArgsConstructor;

/** 프로젝트 태그 JPA 영속성 어댑터. */
@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectPort {

    private final ProjectJpaRepository projectJpaRepository;

    @Override
    public Project save(Project project) {
        return projectJpaRepository.save(project);
    }

    @Override
    public Optional<Project> findByIdAndUserId(Long projectId, Long userId) {
        return projectJpaRepository.findByIdAndUserIdAndIsDeletedFalse(projectId, userId);
    }

    @Override
    public Page<Project> findAllByUserId(Long userId, Pageable pageable) {
        return projectJpaRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable);
    }

    @Override
    public boolean existsByUserIdAndName(Long userId, String name) {
        return projectJpaRepository.existsByUserIdAndNameAndIsDeletedFalse(userId, name);
    }
}