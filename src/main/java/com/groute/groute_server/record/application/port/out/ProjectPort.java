package com.groute.groute_server.record.application.port.out;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.groute.groute_server.record.domain.Project;

/** 프로젝트 태그 영속성 포트. */
public interface ProjectPort {

    Project save(Project project);

    Optional<Project> findByIdAndUserId(Long projectId, Long userId);

    Page<Project> findAllByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndName(Long userId, String name);

    /** 비정규화 카운터(title_count) 증감. increment는 음수 가능. */
    void applyTitleCountIncrement(Long projectId, int increment);
}
